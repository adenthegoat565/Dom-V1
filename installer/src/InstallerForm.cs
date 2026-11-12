using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace DomInstaller
{
    /// <summary>The install screen.</summary>
    internal sealed class InstallerForm : Form
    {
        private enum State { Ready, Installing, Done }

        private readonly Payload _payload;
        private readonly CommandLine _options;
        private readonly HeaderBanner _header;
        private readonly ModList _modList;
        private readonly RamSelector _ram;
        private readonly Label _folderPath;
        private readonly TextLink _changeFolder;
        private readonly SlimProgress _progress;
        private readonly Label _status;
        private readonly FlatButton _install;
        private readonly FlatButton _openMods;
        private readonly Font _sectionFont = Ui.Font(10f, FontStyle.Bold);
        private readonly Font _bodyFont = Ui.Font(9f);
        private string _minecraftDir;
        private State _state = State.Ready;
        private InstallResult _result;
        private CancellationTokenSource _cancel;

        public InstallerForm(Payload payload, CommandLine options)
        {
            _payload = payload;
            _options = options;
            _minecraftDir = options.MinecraftDir ?? Installer.DefaultMinecraftDir();
            Ui.InitScale();

            Text = "Dom Client Installer";
            FormBorderStyle = FormBorderStyle.FixedSingle;
            MaximizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Ui.Background;
            ForeColor = Ui.Text;
            Font = _bodyFont;
            AutoScaleMode = AutoScaleMode.None;
            try
            {
                using (var stream = Payload.Open("assets/icon.ico")) Icon = new Icon(stream);
            }
            catch (Exception)
            {
                // Keep the default icon.
            }

            Image logo = null;
            try
            {
                using (var stream = Payload.Open("assets/logo.png")) logo = new Bitmap(Image.FromStream(stream));
            }
            catch (Exception)
            {
                // No logo, the banner still works.
            }

            // Two columns (mods on the left, settings + Install on the right) so the window stays short
            // enough for 1366x768 laptops and 1080p screens at 150% scaling.
            int pad = Ui.S(24);
            int gap = Ui.S(28);
            int leftWidth = Ui.S(430);
            int rightWidth = Ui.S(330);
            int width = pad + leftWidth + gap + rightWidth + pad;
            int rightX = pad + leftWidth + gap;

            _header = new HeaderBanner(logo)
            {
                Subtitle = "Minecraft " + payload.GameVersion + "  \u2022  Fabric " + payload.LoaderVersion + "  \u2022  Simple Voice Chat + click GUI",
                Bounds = new Rectangle(0, 0, width, Ui.S(96)),
            };
            Controls.Add(_header);
            int top = _header.Bottom + Ui.S(18);

            // Left column: the mod list.
            Controls.Add(MakeLabel("Mods to install", _sectionFont, Ui.Text, new Rectangle(pad, top, leftWidth / 2, Ui.S(22))));
            Controls.Add(MakeLabel("Tick extra mods you want", _bodyFont, Ui.TextFaint, new Rectangle(pad + leftWidth / 2, top, leftWidth / 2, Ui.S(22)), ContentAlignment.MiddleRight));
            var listMods = new List<ModEntry>
            {
                new ModEntry
                {
                    Id = "dom-client",
                    Name = "Dom Client",
                    Description = "Click GUI (Right Shift), HUD, zoom and voice chat buttons.",
                    Required = true,
                    Version = "",
                },
            };
            listMods.AddRange(payload.Mods);
            int listTop = top + Ui.S(28);
            _modList = new ModList(listMods, payload.Mods.Where(m => m.Default).Select(m => m.Id))
            {
                Bounds = new Rectangle(pad, listTop, leftWidth, (int)Math.Ceiling(ModList.RowHeight * Ui.Scale * listMods.Count(m => !m.Hidden)) + 1),
            };
            Controls.Add(_modList);
            int bottom = _modList.Bottom;

            // Right column: settings, status and the big button, lined up with the bottom of the list.
            int y = top;
            Controls.Add(MakeLabel("Memory for Minecraft", _sectionFont, Ui.Text, new Rectangle(rightX, y, rightWidth, Ui.S(22))));
            y += Ui.S(28);
            _ram = new RamSelector(TotalRamGb())
            {
                Bounds = new Rectangle(rightX, y, rightWidth, Ui.S(34)),
            };
            if (options.RamGb.HasValue) _ram.Value = options.RamGb.Value;
            Controls.Add(_ram);
            y = _ram.Bottom + Ui.S(22);

            Controls.Add(MakeLabel("Minecraft folder", _sectionFont, Ui.Text, new Rectangle(rightX, y, rightWidth - Ui.S(100), Ui.S(22))));
            _changeFolder = new TextLink
            {
                Text = "Change\u2026",
                Bounds = new Rectangle(rightX + rightWidth - Ui.S(100), y, Ui.S(100), Ui.S(22)),
            };
            _changeFolder.Click += (s, e) => ChooseFolder();
            Controls.Add(_changeFolder);
            y += Ui.S(24);
            _folderPath = MakeLabel(_minecraftDir, _bodyFont, Ui.TextDim, new Rectangle(rightX, y, rightWidth, Ui.S(22)));
            _folderPath.AutoEllipsis = true;
            Controls.Add(_folderPath);
            y = _folderPath.Bottom + Ui.S(18);

            Controls.Add(MakeLabel("Adds a \"" + Installer.ProfileName + "\" installation to your Minecraft Launcher with its own mods folder. "
                + "Your other installations and worlds aren't touched.", _bodyFont, Ui.TextFaint, new Rectangle(rightX, y, rightWidth, Ui.S(54)), ContentAlignment.TopLeft));

            int buttonTop = bottom - Ui.S(52);
            _progress = new SlimProgress { Bounds = new Rectangle(rightX, buttonTop - Ui.S(84), rightWidth, Ui.S(8)) };
            Controls.Add(_progress);
            _status = MakeLabel("", _bodyFont, Ui.TextDim, new Rectangle(rightX, _progress.Bottom + Ui.S(8), rightWidth, Ui.S(64)), ContentAlignment.TopLeft);
            Controls.Add(_status);

            _openMods = new FlatButton(9.5f)
            {
                Text = "Open mods folder",
                Primary = false,
                Bounds = new Rectangle(rightX, buttonTop, Ui.S(130), Ui.S(52)),
                Visible = false,
            };
            _openMods.Click += (s, e) => OpenModsFolder();
            Controls.Add(_openMods);

            _install = new FlatButton(12f)
            {
                Text = "Install",
                Bounds = new Rectangle(rightX, buttonTop, rightWidth, Ui.S(52)),
            };
            _install.Click += (s, e) => OnInstallClicked();
            Controls.Add(_install);
            y = bottom + Ui.S(24);

            ClientSize = new Size(width, y);
            UpdateReadyStatus();
            _modList.SelectionChanged += (s, e) => UpdateReadyStatus();
        }

        private Label MakeLabel(string text, Font font, Color color, Rectangle bounds, ContentAlignment align = ContentAlignment.MiddleLeft)
        {
            return new Label
            {
                Text = text,
                Font = font,
                ForeColor = color,
                BackColor = Ui.Background,
                TextAlign = align,
                Bounds = bounds,
                UseMnemonic = false,
            };
        }

        protected override void OnHandleCreated(EventArgs e)
        {
            base.OnHandleCreated(e);
            Ui.UseDarkTitleBar(this);
        }

        protected override void OnShown(EventArgs e)
        {
            base.OnShown(e);
            if (Environment.GetCommandLineArgs().Contains("--auto-install")) OnInstallClicked();
        }

        private void UpdateReadyStatus()
        {
            if (_state != State.Ready) return;
            var selected = _payload.Resolve(_modList.Selected);
            long bytes = _payload.Mods.Where(m => selected.Contains(m.Id)).Sum(m => m.Size);
            string size = (bytes / 1048576.0).ToString("0", System.Globalization.CultureInfo.InvariantCulture);
            if (!Directory.Exists(_minecraftDir))
            {
                SetStatus("Minecraft folder not found. Install the Minecraft Launcher and open it once, or pick the folder with Change…", Ui.Error);
            }
            else
            {
                SetStatus("Ready. " + (selected.Count + 1) + " mods (about " + size + " MB to download). A new \"" + Installer.ProfileName
                    + "\" installation will appear in your Minecraft Launcher.", Ui.TextDim);
            }
        }

        private void SetStatus(string text, Color color)
        {
            _status.Text = text;
            _status.ForeColor = color;
        }

        private void ChooseFolder()
        {
            using (var dialog = new FolderBrowserDialog())
            {
                dialog.Description = "Pick your .minecraft folder (the one with launcher_profiles.json in it).";
                dialog.SelectedPath = Directory.Exists(_minecraftDir) ? _minecraftDir : Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
                dialog.ShowNewFolderButton = false;
                if (dialog.ShowDialog(this) == DialogResult.OK)
                {
                    _minecraftDir = dialog.SelectedPath;
                    _folderPath.Text = _minecraftDir;
                    UpdateReadyStatus();
                }
            }
        }

        private async void OnInstallClicked()
        {
            if (_state == State.Installing) return;
            if (_state == State.Done)
            {
                if (!LauncherHelper.TryOpen())
                {
                    MessageBox.Show(this, "Open the Minecraft Launcher from the Start menu, pick \"" + Installer.ProfileName + "\" next to the Play button, and press Play.",
                        "Open the Minecraft Launcher", MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                return;
            }

            if (!Directory.Exists(_minecraftDir) || LauncherProfiles.FindProfileFiles(_minecraftDir).Count == 0)
            {
                var answer = MessageBox.Show(this,
                    "It looks like the Minecraft Launcher hasn't been opened on this PC yet (no launcher_profiles.json in\n" + _minecraftDir + ").\n\n"
                    + "Best: open the Minecraft Launcher once, sign in, close it, then press Install again.\n\nInstall anyway?",
                    "Minecraft not found", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (answer != DialogResult.Yes) return;
            }

            var running = Installer.RunningLaunchers();
            if (running.Count > 0)
            {
                var answer = MessageBox.Show(this,
                    "The Minecraft Launcher is open. Please close it now, then press OK.\n\n"
                    + "(If you install while it's open, just restart the launcher afterwards so it shows \"" + Installer.ProfileName + "\".)",
                    "Close the Minecraft Launcher", MessageBoxButtons.OKCancel, MessageBoxIcon.Information);
                if (answer != DialogResult.OK) return;
            }

            var install = new InstallOptions
            {
                MinecraftDir = _minecraftDir,
                GameDir = _options.GameDir ?? Installer.DefaultGameDir(_minecraftDir),
                RamGb = _ram.Value,
                SelectedMods = new HashSet<string>(_modList.Selected, StringComparer.Ordinal),
            };

            _state = State.Installing;
            SetInputsEnabled(false);
            _install.Text = "Installing…";
            _progress.Fill = Color.Empty;
            _progress.Value = 0;
            _cancel = new CancellationTokenSource();
            var installer = new Installer(_payload);
            installer.Progress += (message, fraction) =>
            {
                if (IsHandleCreated) BeginInvoke(new Action(() =>
                {
                    _progress.Value = fraction;
                    SetStatus(message, Ui.TextDim);
                }));
            };

            try
            {
                _result = await Task.Run(() => installer.Run(install, _cancel.Token));
                _state = State.Done;
                _progress.Value = 1;
                _progress.Fill = Ui.Success;
                string message = "Installed! Open the Minecraft Launcher, pick \"" + Installer.ProfileName + "\" next to the Play button and press Play. "
                    + "Press Right Shift in game for the click GUI.";
                if (_result.Warnings.Count > 0) message += " (" + _result.Warnings.Count + " library download(s) will be finished by the launcher.)";
                SetStatus(message, Ui.Success);
                _install.Text = "Open Launcher";
                _install.Enabled = true;
                _openMods.Visible = true;
                int spacing = Ui.S(10);
                _install.Bounds = new Rectangle(_openMods.Right + spacing, _install.Top, _status.Right - _openMods.Right - spacing, _install.Height);
            }
            catch (Exception e)
            {
                _state = State.Ready;
                _progress.Fill = Ui.Error;
                SetStatus("Install failed: " + e.Message, Ui.Error);
                SetInputsEnabled(true);
                _install.Text = "Try again";
                MessageBox.Show(this, "Something went wrong:\n\n" + e.Message + "\n\nCheck your internet connection and try again. "
                    + "Make sure Minecraft is closed.", "Install failed", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private void SetInputsEnabled(bool enabled)
        {
            _install.Enabled = enabled;
            _modList.Enabled = enabled;
            _ram.Enabled = enabled;
            _changeFolder.Enabled = enabled;
        }

        private void OpenModsFolder()
        {
            string dir = _result != null ? _result.ModsDir : Path.Combine(Installer.DefaultGameDir(_minecraftDir), "mods");
            try
            {
                Directory.CreateDirectory(dir);
                Process.Start(new ProcessStartInfo(dir) { UseShellExecute = true });
            }
            catch (Exception e)
            {
                MessageBox.Show(this, "Couldn't open " + dir + ": " + e.Message, "Mods folder", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            if (_state == State.Installing)
            {
                var answer = MessageBox.Show(this, "The install is still running. Stop it?", "Dom Client Installer", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (answer != DialogResult.Yes)
                {
                    e.Cancel = true;
                    return;
                }
                if (_cancel != null) _cancel.Cancel();
            }
            base.OnFormClosing(e);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _sectionFont.Dispose();
                _bodyFont.Dispose();
                if (_cancel != null) _cancel.Dispose();
            }
            base.Dispose(disposing);
        }

        [StructLayout(LayoutKind.Sequential)]
        private struct MemoryStatus
        {
            public uint Length;
            public uint MemoryLoad;
            public ulong TotalPhys;
            public ulong AvailPhys;
            public ulong TotalPageFile;
            public ulong AvailPageFile;
            public ulong TotalVirtual;
            public ulong AvailVirtual;
            public ulong AvailExtendedVirtual;
        }

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool GlobalMemoryStatusEx(ref MemoryStatus status);

        private static int TotalRamGb()
        {
            try
            {
                if (Environment.OSVersion.Platform == PlatformID.Win32NT)
                {
                    var status = new MemoryStatus { Length = (uint)Marshal.SizeOf(typeof(MemoryStatus)) };
                    if (GlobalMemoryStatusEx(ref status)) return (int)Math.Round(status.TotalPhys / 1073741824.0);
                }
                else if (File.Exists("/proc/meminfo"))
                {
                    string line = File.ReadLines("/proc/meminfo").FirstOrDefault(l => l.StartsWith("MemTotal:", StringComparison.Ordinal));
                    long kb;
                    if (line != null && long.TryParse(new string(line.Where(char.IsDigit).ToArray()), out kb)) return (int)Math.Round(kb / 1048576.0);
                }
            }
            catch (Exception)
            {
                // Fall through to a sensible guess.
            }
            return 8;
        }
    }

    internal static class LauncherHelper
    {
        private const string StorePackage = "Microsoft.4297127D64EC6_8wekyb3d8bbwe";

        /// <summary>Starts the Minecraft Launcher (classic install or Microsoft Store / Xbox app).</summary>
        public static bool TryOpen()
        {
            var candidates = new[]
            {
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "Minecraft Launcher", "MinecraftLauncher.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Minecraft Launcher", "MinecraftLauncher.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "Minecraft", "MinecraftLauncher.exe"),
            };
            foreach (var exe in candidates)
            {
                try
                {
                    if (File.Exists(exe))
                    {
                        Process.Start(new ProcessStartInfo(exe) { UseShellExecute = true });
                        return true;
                    }
                }
                catch (Exception)
                {
                    // Try the next one.
                }
            }
            try
            {
                string packages = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Packages", StorePackage);
                if (Directory.Exists(packages))
                {
                    Process.Start(new ProcessStartInfo("explorer.exe", "shell:AppsFolder\\" + StorePackage + "!Minecraft") { UseShellExecute = true });
                    return true;
                }
            }
            catch (Exception)
            {
                // Fall back to telling the user.
            }
            return false;
        }
    }
}
