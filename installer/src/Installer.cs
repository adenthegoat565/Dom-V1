using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;

namespace DomInstaller
{
    public sealed class InstallOptions
    {
        public string MinecraftDir;
        public string GameDir;
        public int RamGb = 4;
        public HashSet<string> SelectedMods = new HashSet<string>(StringComparer.Ordinal);
        public bool CopySettings = true;
    }

    public sealed class InstallResult
    {
        public string ProfileName;
        public string GameDir;
        public string ModsDir;
        public List<string> ProfileFiles = new List<string>();
        public List<string> Warnings = new List<string>();
        public int ModCount;
    }

    /// <summary>Does the actual install. Runs on a background thread; reports progress as (message, 0..1).</summary>
    public sealed class Installer
    {
        public const string ProfileName = "Dom Client 26.2";
        private const string ManifestName = ".dom-client-installed.json";

        private readonly Payload _payload;
        public event Action<string, double> Progress;

        public Installer(Payload payload)
        {
            _payload = payload;
        }

        public static string DefaultMinecraftDir()
        {
            string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            return Path.Combine(appData, ".minecraft");
        }

        public static string DefaultGameDir(string minecraftDir)
        {
            return Path.Combine(minecraftDir, "DomClient-26.2");
        }

        /// <summary>Names of Minecraft Launcher processes that are running right now (empty if none).</summary>
        public static List<string> RunningLaunchers()
        {
            var found = new List<string>();
            foreach (var name in new[] { "MinecraftLauncher", "Minecraft" })
            {
                try
                {
                    var processes = Process.GetProcessesByName(name);
                    if (processes.Length > 0) found.Add(name + ".exe");
                    foreach (var p in processes) p.Dispose();
                }
                catch (Exception)
                {
                    // Not allowed to list processes; just skip the check.
                }
            }
            return found;
        }

        private void Report(string message, double fraction)
        {
            var handler = Progress;
            if (handler != null) handler(message, Math.Max(0, Math.Min(1, fraction)));
        }

        public InstallResult Run(InstallOptions options, CancellationToken cancel)
        {
            var result = new InstallResult { ProfileName = ProfileName, GameDir = options.GameDir };
            string mcDir = options.MinecraftDir;
            Directory.CreateDirectory(mcDir);

            // 1. Fabric Loader as a launcher "version".
            Report("Installing Fabric Loader " + _payload.LoaderVersion + "...", 0.01);
            InstallFabricVersion(mcDir);

            // 2. Fabric's libraries. The launcher can fetch these too, so a failure here is only a warning.
            InstallLibraries(mcDir, result, cancel, 0.02, 0.12);

            // 3. Mods.
            string modsDir = Path.Combine(options.GameDir, "mods");
            Directory.CreateDirectory(modsDir);
            result.ModsDir = modsDir;
            var wanted = _payload.Resolve(options.SelectedMods);
            var mods = _payload.Mods.Where(m => wanted.Contains(m.Id)).ToList();
            var installedFiles = new List<string>();
            long totalBytes = Math.Max(1, mods.Sum(m => Math.Max(1, m.Size)));
            long doneBytes = 0;
            foreach (var mod in mods)
            {
                cancel.ThrowIfCancellationRequested();
                string target = Path.Combine(modsDir, mod.FileName);
                double start = 0.12 + 0.75 * doneBytes / totalBytes;
                double span = 0.75 * Math.Max(1, mod.Size) / totalBytes;
                if (File.Exists(target) && SafeEquals(Downloader.Sha512(target), mod.Sha512))
                {
                    Report(mod.Name + " is already up to date", start + span);
                }
                else
                {
                    Report("Downloading " + mod.Name + "...", start);
                    byte[] data = Downloader.Download(mod.Url, (got, total) =>
                    {
                        long size = total > 0 ? total : Math.Max(1, mod.Size);
                        Report("Downloading " + mod.Name + " (" + Mb(got) + " / " + Mb(size) + ")", start + span * Math.Min(1.0, (double)got / size));
                    }, cancel);
                    if (!SafeEquals(Downloader.Sha512(data), mod.Sha512))
                    {
                        throw new IOException(mod.Name + " didn't download correctly (checksum mismatch). Please try again.");
                    }
                    Downloader.WriteAtomic(target, data);
                }
                installedFiles.Add(mod.FileName);
                doneBytes += Math.Max(1, mod.Size);
            }

            // 4. The Dom Client click GUI mod itself, which is inside this .exe.
            Report("Installing Dom Client...", 0.88);
            Downloader.WriteAtomic(Path.Combine(modsDir, _payload.DomClientFileName), _payload.DomClientJar);
            installedFiles.Add(_payload.DomClientFileName);
            result.ModCount = installedFiles.Count;

            // 5. Remove files from an older install that aren't part of this one (old versions, unticked mods).
            CleanUpOldFiles(modsDir, installedFiles, result);
            WriteManifest(modsDir, installedFiles);

            // 6. Bring over keybinds/settings and the server list the first time.
            if (options.CopySettings)
            {
                CopyIfMissing(Path.Combine(mcDir, "options.txt"), Path.Combine(options.GameDir, "options.txt"));
                CopyIfMissing(Path.Combine(mcDir, "servers.dat"), Path.Combine(options.GameDir, "servers.dat"));
            }

            // 7. The launcher installation.
            Report("Adding \"" + ProfileName + "\" to the Minecraft Launcher...", 0.94);
            result.ProfileFiles = LauncherProfiles.Install(mcDir, ProfileName, _payload.VersionId, options.GameDir, options.RamGb, _payload.ProfileIcon);

            Report("Done!", 1.0);
            return result;
        }

        private void InstallFabricVersion(string mcDir)
        {
            string id = _payload.VersionId;
            string versionDir = Path.Combine(mcDir, "versions", id);
            Directory.CreateDirectory(versionDir);
            // Same as Fabric's own installer: the profile json, and no jar (it comes from the vanilla version).
            string jar = Path.Combine(versionDir, id + ".jar");
            if (File.Exists(jar)) File.Delete(jar);
            Downloader.WriteAtomic(Path.Combine(versionDir, id + ".json"), new UTF8Encoding(false).GetBytes(Json.Write(_payload.FabricProfile)));
        }

        private void InstallLibraries(string mcDir, InstallResult result, CancellationToken cancel, double from, double to)
        {
            var libraries = (_payload.FabricProfile["libraries"] as List<object> ?? new List<object>()).OfType<JsonObject>().ToList();
            int index = 0;
            foreach (var lib in libraries)
            {
                cancel.ThrowIfCancellationRequested();
                index++;
                string name = lib.GetString("name");
                string baseUrl = lib.GetString("url") ?? "https://maven.fabricmc.net/";
                string sha1 = lib.GetString("sha1");
                if (string.IsNullOrEmpty(name)) continue;
                string relative = MavenPath(name);
                if (relative == null) continue;
                string target = Path.Combine(mcDir, "libraries", relative.Replace('/', Path.DirectorySeparatorChar));
                Report("Getting Fabric library " + index + " of " + libraries.Count + "...", from + (to - from) * index / Math.Max(1, libraries.Count));
                if (File.Exists(target) && (string.IsNullOrEmpty(sha1) || SafeEquals(Downloader.Sha1(target), sha1))) continue;
                try
                {
                    if (!baseUrl.EndsWith("/", StringComparison.Ordinal)) baseUrl += "/";
                    byte[] data = Downloader.Download(baseUrl + relative, null, cancel);
                    if (!string.IsNullOrEmpty(sha1) && !SafeEquals(Downloader.Sha1(data), sha1))
                        throw new IOException("checksum mismatch");
                    Downloader.WriteAtomic(target, data);
                }
                catch (OperationCanceledException)
                {
                    throw;
                }
                catch (Exception e)
                {
                    result.Warnings.Add("Couldn't pre-download " + name + " (" + e.Message + "). The Minecraft Launcher will download it instead.");
                }
            }
        }

        /// <summary>group:artifact:version → group/path/artifact/version/artifact-version.jar</summary>
        public static string MavenPath(string coordinates)
        {
            var parts = coordinates.Split(':');
            if (parts.Length < 3) return null;
            string group = parts[0].Replace('.', '/');
            string artifact = parts[1];
            string version = parts[2];
            string classifier = parts.Length > 3 ? "-" + parts[3] : "";
            return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
        }

        private static void CleanUpOldFiles(string modsDir, List<string> keep, InstallResult result)
        {
            string manifest = Path.Combine(modsDir, ManifestName);
            if (!File.Exists(manifest)) return;
            try
            {
                var old = Json.ParseObject(File.ReadAllText(manifest, Encoding.UTF8))["files"] as List<object>;
                if (old == null) return;
                foreach (var file in old.OfType<string>())
                {
                    if (keep.Contains(file, StringComparer.OrdinalIgnoreCase)) continue;
                    if (file.IndexOfAny(new[] { '/', '\\' }) >= 0 || file.Contains("..")) continue;
                    string path = Path.Combine(modsDir, file);
                    if (File.Exists(path)) File.Delete(path);
                }
            }
            catch (Exception e)
            {
                result.Warnings.Add("Couldn't remove old mod files: " + e.Message);
            }
        }

        private static void WriteManifest(string modsDir, List<string> files)
        {
            var root = new JsonObject();
            root["installer"] = "Dom-V1";
            root["files"] = files.Cast<object>().ToList();
            File.WriteAllText(Path.Combine(modsDir, ManifestName), Json.Write(root), new UTF8Encoding(false));
        }

        private static void CopyIfMissing(string from, string to)
        {
            try
            {
                if (File.Exists(from) && !File.Exists(to)) File.Copy(from, to);
            }
            catch (IOException)
            {
                // Nice to have only.
            }
        }

        private static bool SafeEquals(string a, string b)
        {
            return a != null && b != null && string.Equals(a.Trim(), b.Trim(), StringComparison.OrdinalIgnoreCase);
        }

        private static string Mb(long bytes)
        {
            return (bytes / 1048576.0).ToString("0.0", System.Globalization.CultureInfo.InvariantCulture) + " MB";
        }
    }
}
