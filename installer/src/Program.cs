using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;

namespace DomInstaller
{
    internal static class Program
    {
        [DllImport("kernel32.dll")]
        private static extern bool AttachConsole(int processId);

        [STAThread]
        private static int Main(string[] args)
        {
            var options = CommandLine.Parse(args);
            if (options.Silent)
            {
                if (Environment.OSVersion.Platform == PlatformID.Win32NT) AttachConsole(-1);
                return RunSilent(options);
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Payload payload;
            try
            {
                payload = Payload.Load();
            }
            catch (Exception e)
            {
                MessageBox.Show("This installer is damaged: " + e.Message + "\n\nPlease download it again.", "Dom Client Installer",
                    MessageBoxButtons.OK, MessageBoxIcon.Error);
                return 1;
            }
            Application.Run(new InstallerForm(payload, options));
            return 0;
        }

        /// <summary>Installs without a window, for scripts and automated tests: --silent [--minecraft-dir DIR] [--ram 4] [--mods a,b] [--log FILE].</summary>
        private static int RunSilent(CommandLine options)
        {
            System.IO.StreamWriter log = null;
            if (!string.IsNullOrEmpty(options.LogPath))
            {
                log = new System.IO.StreamWriter(options.LogPath, false, new System.Text.UTF8Encoding(false)) { AutoFlush = true };
                Console.SetOut(log);
                Console.SetError(log);
            }
            try
            {
                var payload = Payload.Load();
                string mcDir = options.MinecraftDir ?? Installer.DefaultMinecraftDir();
                var install = new InstallOptions
                {
                    MinecraftDir = mcDir,
                    GameDir = options.GameDir ?? Installer.DefaultGameDir(mcDir),
                    RamGb = options.RamGb ?? 4,
                    SelectedMods = new HashSet<string>(options.Mods ?? payload.Mods.Where(m => m.Default).Select(m => m.Id), StringComparer.Ordinal),
                };
                var installer = new Installer(payload);
                string lastMessage = null;
                installer.Progress += (message, fraction) =>
                {
                    if (message == lastMessage) return;
                    lastMessage = message;
                    Console.WriteLine("[{0,3:0}%] {1}", fraction * 100, message);
                };
                var result = installer.Run(install, CancellationToken.None);
                foreach (var warning in result.Warnings) Console.WriteLine("Warning: " + warning);
                Console.WriteLine("Installed {0} mods into {1}", result.ModCount, result.ModsDir);
                Console.WriteLine("Launcher profile \"{0}\" written to: {1}", result.ProfileName, string.Join(", ", result.ProfileFiles));
                return 0;
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Install failed: " + e);
                return 1;
            }
            finally
            {
                if (log != null) log.Dispose();
            }
        }
    }

    internal sealed class CommandLine
    {
        public bool Silent;
        public string MinecraftDir;
        public string GameDir;
        public int? RamGb;
        public List<string> Mods;
        public string LogPath;

        public static CommandLine Parse(string[] args)
        {
            var result = new CommandLine();
            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i];
                string next = i + 1 < args.Length ? args[i + 1] : null;
                switch (arg)
                {
                    case "--silent":
                        result.Silent = true;
                        break;
                    case "--minecraft-dir":
                        result.MinecraftDir = next;
                        i++;
                        break;
                    case "--game-dir":
                        result.GameDir = next;
                        i++;
                        break;
                    case "--ram":
                        int ram;
                        if (int.TryParse(next, out ram)) result.RamGb = ram;
                        i++;
                        break;
                    case "--mods":
                        result.Mods = (next ?? "").Split(new[] { ',' }, StringSplitOptions.RemoveEmptyEntries).Select(s => s.Trim()).ToList();
                        i++;
                        break;
                    case "--log":
                        result.LogPath = next;
                        i++;
                        break;
                }
            }
            return result;
        }
    }
}
