using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace DomInstaller
{
    /// <summary>Colours, fonts, DPI scaling and drawing helpers shared by the installer window.</summary>
    internal static class Ui
    {
        public static float Scale = 1f;

        public static readonly Color Background = Color.FromArgb(14, 14, 19);
        public static readonly Color Card = Color.FromArgb(24, 24, 32);
        public static readonly Color CardHover = Color.FromArgb(31, 31, 42);
        public static readonly Color Border = Color.FromArgb(44, 44, 56);
        public static readonly Color Text = Color.FromArgb(244, 244, 245);
        public static readonly Color TextDim = Color.FromArgb(161, 161, 170);
        public static readonly Color TextFaint = Color.FromArgb(113, 113, 122);
        public static readonly Color Accent = Color.FromArgb(139, 92, 246);
        public static readonly Color Accent2 = Color.FromArgb(59, 130, 246);
        public static readonly Color Success = Color.FromArgb(74, 222, 128);
        public static readonly Color Error = Color.FromArgb(248, 113, 113);

        public static readonly string FontName = "Segoe UI";

        public static int S(float value)
        {
            return (int)Math.Round(value * Scale);
        }

        public static void InitScale()
        {
            try
            {
                using (var g = Graphics.FromHwnd(IntPtr.Zero)) Scale = Math.Max(1f, g.DpiX / 96f);
            }
            catch (Exception)
            {
                Scale = 1f;
            }
        }

        public static Font Font(float points, FontStyle style = FontStyle.Regular)
        {
            return new Font(FontName, points, style, GraphicsUnit.Point);
        }

        public static void Smooth(Graphics g)
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
        }

        public static GraphicsPath RoundRect(RectangleF r, float radius)
        {
            var path = new GraphicsPath();
            float d = Math.Max(1f, Math.Min(radius * 2, Math.Min(r.Width, r.Height)));
            path.AddArc(r.X, r.Y, d, d, 180, 90);
            path.AddArc(r.Right - d, r.Y, d, d, 270, 90);
            path.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
            path.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
            path.CloseFigure();
            return path;
        }

        public static LinearGradientBrush AccentBrush(RectangleF r)
        {
            if (r.Width < 1) r.Width = 1;
            if (r.Height < 1) r.Height = 1;
            return new LinearGradientBrush(r, Accent, Accent2, LinearGradientMode.ForwardDiagonal);
        }

        public static void DrawText(Graphics g, string text, Font font, Color color, RectangleF rect,
            StringAlignment horizontal = StringAlignment.Near, StringAlignment vertical = StringAlignment.Center)
        {
            using (var brush = new SolidBrush(color))
            using (var format = new StringFormat(StringFormatFlags.NoWrap))
            {
                format.Alignment = horizontal;
                format.LineAlignment = vertical;
                format.Trimming = StringTrimming.EllipsisCharacter;
                g.DrawString(text, font, brush, rect, format);
            }
        }

        public static void DrawWrapped(Graphics g, string text, Font font, Color color, RectangleF rect)
        {
            using (var brush = new SolidBrush(color))
            using (var format = new StringFormat())
            {
                format.Trimming = StringTrimming.EllipsisWord;
                g.DrawString(text, font, brush, rect, format);
            }
        }

        [DllImport("dwmapi.dll")]
        private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attribute, ref int value, int size);

        /// <summary>Dark title bar on Windows 10 (2004+) and 11. Does nothing anywhere else.</summary>
        public static void UseDarkTitleBar(Form form)
        {
            if (Environment.OSVersion.Platform != PlatformID.Win32NT) return;
            try
            {
                int on = 1;
                if (DwmSetWindowAttribute(form.Handle, 20, ref on, sizeof(int)) != 0)
                    DwmSetWindowAttribute(form.Handle, 19, ref on, sizeof(int));
            }
            catch (Exception)
            {
                // Older Windows: keep the normal title bar.
            }
        }
    }

    /// <summary>Base for the custom-drawn controls: double buffered, transparent to the dark background.</summary>
    internal abstract class PaintedControl : Control
    {
        protected PaintedControl()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw
                | ControlStyles.UserPaint | ControlStyles.SupportsTransparentBackColor, true);
            BackColor = Ui.Background;
            ForeColor = Ui.Text;
        }
    }
}
