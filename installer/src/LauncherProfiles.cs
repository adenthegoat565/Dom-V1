using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;

namespace DomInstaller
{
    /// <summary>Adds (or updates) the "Dom Client" installation in the official Minecraft Launcher.</summary>
    public static class LauncherProfiles
    {
        public const string ProfileKey = "dom-client-26.2";

        /// <summary>The classic launcher and the Microsoft Store / Xbox app launcher keep separate files.</summary>
        public static readonly string[] ProfileFiles = { "launcher_profiles.json", "launcher_profiles_microsoft_store.json" };

        public const string DefaultJavaArgs = "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M";

        public static List<string> FindProfileFiles(string minecraftDir)
        {
            return ProfileFiles.Select(f => Path.Combine(minecraftDir, f)).Where(File.Exists).ToList();
        }

        /// <summary>Writes the installation into every launcher profile file that exists. Returns the files changed.</summary>
        public static List<string> Install(string minecraftDir, string profileName, string versionId, string gameDir, int ramGb, byte[] iconPng)
        {
            var files = FindProfileFiles(minecraftDir);
            if (files.Count == 0)
            {
                // The launcher hasn't been opened on this PC yet. Start a file it will pick up.
                string path = Path.Combine(minecraftDir, ProfileFiles[0]);
                var fresh = new JsonObject();
                fresh["profiles"] = new JsonObject();
                fresh["settings"] = new JsonObject();
                fresh["version"] = new JsonNumber(3);
                File.WriteAllText(path, Json.Write(fresh), new UTF8Encoding(false));
                files.Add(path);
            }

            string now = DateTime.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'", CultureInfo.InvariantCulture);
            string icon = "data:image/png;base64," + Convert.ToBase64String(iconPng);
            string javaArgs = "-Xmx" + ramGb + "G " + DefaultJavaArgs;

            foreach (var path in files)
            {
                string text = File.ReadAllText(path, Encoding.UTF8);
                JsonObject root;
                try
                {
                    root = Json.ParseObject(text);
                }
                catch (JsonException e)
                {
                    throw new IOException(Path.GetFileName(path) + " looks damaged, so it was left alone (" + e.Message + "). "
                        + "Open the Minecraft Launcher once to repair it, then run this installer again.", e);
                }

                // Keep one backup of the file as it was before we ever touched it.
                string backup = path + ".before-dom-client";
                if (!File.Exists(backup)) File.WriteAllText(backup, text, new UTF8Encoding(false));

                var profiles = root.GetObject("profiles");
                if (profiles == null)
                {
                    profiles = new JsonObject();
                    root["profiles"] = profiles;
                }
                var profile = profiles.GetObject(ProfileKey);
                if (profile == null)
                {
                    profile = new JsonObject();
                    profile["created"] = now;
                    profiles[ProfileKey] = profile;
                }
                profile["name"] = profileName;
                profile["type"] = "custom";
                profile["lastVersionId"] = versionId;
                profile["gameDir"] = gameDir;
                profile["icon"] = icon;
                profile["javaArgs"] = javaArgs;
                // Most recently used goes first in the launcher, so the new install is selected next to Play.
                profile["lastUsed"] = now;

                string temp = path + ".tmp";
                File.WriteAllText(temp, Json.Write(root), new UTF8Encoding(false));
                File.Copy(temp, path, true);
                File.Delete(temp);
            }
            return files;
        }
    }
}
