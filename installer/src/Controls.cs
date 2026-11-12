using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Linq;
using System.Windows.Forms;

namespace DomInstaller
{
    /// <summary>The coloured banner at the top with the logo and title.</summary>
    internal sealed class HeaderBanner : PaintedControl
    {
        private readonly Image _logo;
        private readonly Font _title = Ui.Font(20f, FontStyle.Bold);
        private readonly Font _subtitle = Ui.Font(9.5f);
        public string Subtitle = "";

        public HeaderBanner(Image logo)
        {
            _logo = logo;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            Ui.Smooth(g);
            var rect = new Rectangle(0, 0, Width, Height);
            using (var brush = new LinearGradientBrush(rect, Color.FromArgb(76, 29, 149), Color.FromArgb(30, 64, 175), LinearGradientMode.Horizontal))
            {
                g.FillRectangle(brush, rect);
            }
            // Soft glow behind the logo.
            using (var glow = new GraphicsPath())
            {
                glow.AddEllipse(Ui.S(-40), Ui.S(-60), Ui.S(220), Ui.S(220));
                using (var brush = new PathGradientBrush(glow) { CenterColor = Color.FromArgb(70, 255, 255, 255), SurroundColors = new[] { Color.FromArgb(0, 255, 255, 255) } })
                    g.FillPath(brush, glow);
            }
            int logoSize = Ui.S(64);
            int logoY = (Height - logoSize) / 2;
            if (_logo != null) g.DrawImage(_logo, new Rectangle(Ui.S(24), logoY, logoSize, logoSize));
            float textX = Ui.S(104);
            Ui.DrawText(g, "Dom Client", _title, Color.White, new RectangleF(textX, logoY - Ui.S(4), Width - textX - Ui.S(16), Ui.S(40)), StringAlignment.Near, StringAlignment.Near);
            Ui.DrawText(g, Subtitle, _subtitle, Color.FromArgb(225, 225, 245), new RectangleF(textX + Ui.S(2), logoY + Ui.S(38), Width - textX - Ui.S(16), Ui.S(24)), StringAlignment.Near, StringAlignment.Near);
            using (var line = new Pen(Color.FromArgb(60, 255, 255, 255))) g.DrawLine(line, 0, Height - 1, Width, Height - 1);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _title.Dispose();
                _subtitle.Dispose();
            }
            base.Dispose(disposing);
        }
    }

    /// <summary>The list of mods with tick boxes. Required mods are always ticked.</summary>
    internal sealed class ModList : PaintedControl
    {
        public const float RowHeight = 38f;
        private readonly List<ModEntry> _mods;
        private readonly List<ModEntry> _all;
        private readonly HashSet<string> _checked;
        private readonly Font _name = Ui.Font(9.75f, FontStyle.Bold);
        private readonly Font _small = Ui.Font(8.25f);
        private int _hover = -1;
        public event EventHandler SelectionChanged;

        public ModList(List<ModEntry> all, IEnumerable<string> initiallyChecked)
        {
            _all = all;
            _mods = all.Where(m => !m.Hidden).OrderByDescending(m => m.Required).ToList();
            _checked = new HashSet<string>(initiallyChecked, StringComparer.Ordinal);
            foreach (var m in _mods.Where(m => m.Required)) _checked.Add(m.Id);
            Cursor = Cursors.Hand;
        }

        public int VisibleCount { get { return _mods.Count; } }

        public IEnumerable<string> Selected { get { return _checked.ToList(); } }

        public IEnumerable<ModEntry> VisibleMods { get { return _mods; } }

        private int RowAt(int y)
        {
            int row = (int)(y / (RowHeight * Ui.Scale));
            return row >= 0 && row < _mods.Count ? row : -1;
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            base.OnMouseMove(e);
            int row = RowAt(e.Y);
            if (row != _hover)
            {
                _hover = row;
                Cursor = row >= 0 && !_mods[row].Required && Enabled ? Cursors.Hand : Cursors.Default;
                Invalidate();
            }
        }

        protected override void OnMouseLeave(EventArgs e)
        {
            base.OnMouseLeave(e);
            _hover = -1;
            Invalidate();
        }

        protected override void OnMouseClick(MouseEventArgs e)
        {
            base.OnMouseClick(e);
            if (!Enabled || e.Button != MouseButtons.Left) return;
            int row = RowAt(e.Y);
            if (row < 0 || _mods[row].Required) return;
            Toggle(_mods[row]);
        }

        private void Toggle(ModEntry mod)
        {
            if (_checked.Contains(mod.Id))
            {
                // Unticking a mod also unticks the mods that need it (Iris needs Sodium).
                var remove = new Queue<string>();
                remove.Enqueue(mod.Id);
                while (remove.Count > 0)
                {
                    string id = remove.Dequeue();
                    if (!_checked.Remove(id)) continue;
                    foreach (var dependent in _all.Where(m => m.Requires.Contains(id) && !m.Required)) remove.Enqueue(dependent.Id);
                }
            }
            else
            {
                // Ticking a mod also ticks what it needs.
                var add = new Queue<string>();
                add.Enqueue(mod.Id);
                while (add.Count > 0)
                {
                    string id = add.Dequeue();
                    if (!_checked.Add(id)) continue;
                    var entry = _all.FirstOrDefault(m => m.Id == id);
                    if (entry != null) foreach (var dep in entry.Requires) add.Enqueue(dep);
                }
            }
            Invalidate();
            var handler = SelectionChanged;
            if (handler != null) handler(this, EventArgs.Empty);
        }

        protected override void OnEnabledChanged(EventArgs e)
        {
            base.OnEnabledChanged(e);
            Invalidate();
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            Ui.Smooth(g);
            var outer = new RectangleF(0.5f, 0.5f, Width - 1.5f, Height - 1.5f);
            using (var path = Ui.RoundRect(outer, Ui.S(10)))
            using (var fill = new SolidBrush(Ui.Card))
            using (var border = new Pen(Ui.Border))
            {
                g.FillPath(fill, path);
                g.SetClip(path);
                float rowH = RowHeight * Ui.Scale;
                for (int i = 0; i < _mods.Count; i++)
                {
                    var mod = _mods[i];
                    var row = new RectangleF(0, i * rowH, Width, rowH);
                    if (i == _hover && Enabled && !mod.Required)
                    {
                        using (var hover = new SolidBrush(Ui.CardHover)) g.FillRectangle(hover, row);
                    }
                    if (i > 0)
                    {
                        using (var sep = new Pen(Color.FromArgb(34, 34, 45))) g.DrawLine(sep, Ui.S(14), row.Y, Width - Ui.S(14), row.Y);
                    }
                    DrawRow(g, mod, row);
                }
                g.ResetClip();
                g.DrawPath(border, path);
            }
        }

        private void DrawRow(Graphics g, ModEntry mod, RectangleF row)
        {
            bool on = _checked.Contains(mod.Id);
            float box = Ui.S(18);
            var boxRect = new RectangleF(Ui.S(16), row.Y + (row.Height - box) / 2, box, box);
            using (var path = Ui.RoundRect(boxRect, Ui.S(5)))
            {
                if (on)
                {
                    if (mod.Required || !Enabled)
                    {
                        using (var fill = new SolidBrush(Color.FromArgb(91, 72, 160))) g.FillPath(fill, path);
                    }
                    else
                    {
                        using (var fill = Ui.AccentBrush(boxRect)) g.FillPath(fill, path);
                    }
                    using (var tick = new Pen(Color.White, Math.Max(1.6f, 2f * Ui.Scale)) { StartCap = LineCap.Round, EndCap = LineCap.Round, LineJoin = LineJoin.Round })
                    {
                        g.DrawLines(tick, new[]
                        {
                            new PointF(boxRect.X + box * 0.24f, boxRect.Y + box * 0.52f),
                            new PointF(boxRect.X + box * 0.43f, boxRect.Y + box * 0.70f),
                            new PointF(boxRect.X + box * 0.77f, boxRect.Y + box * 0.32f),
                        });
                    }
                }
                else
                {
                    using (var pen = new Pen(Color.FromArgb(82, 82, 96), Math.Max(1f, 1.5f * Ui.Scale))) g.DrawPath(pen, path);
                }
            }

            float textX = boxRect.Right + Ui.S(12);
            float right = Width - Ui.S(16);
            string tag = mod.Required ? "Required" : mod.Version;
            float tagWidth = 0;
            if (mod.Required)
            {
                var size = g.MeasureString(tag, _small);
                tagWidth = size.Width + Ui.S(12);
                var pill = new RectangleF(right - tagWidth, row.Y + (row.Height - Ui.S(20)) / 2, tagWidth, Ui.S(20));
                using (var path = Ui.RoundRect(pill, Ui.S(10)))
                using (var fill = new SolidBrush(Color.FromArgb(46, 37, 84)))
                    g.FillPath(fill, path);
                Ui.DrawText(g, tag, _small, Color.FromArgb(196, 181, 253), pill, StringAlignment.Center);
            }
            Color nameColor = Enabled || on ? Ui.Text : Ui.TextDim;
            Ui.DrawText(g, mod.Name, _name, nameColor, new RectangleF(textX, row.Y + Ui.S(3), right - textX - tagWidth - Ui.S(8), row.Height / 2), StringAlignment.Near, StringAlignment.Far);
            Ui.DrawText(g, mod.Description, _small, Ui.TextDim, new RectangleF(textX, row.Y + row.Height / 2, right - textX - tagWidth - Ui.S(8), row.Height / 2 - Ui.S(3)), StringAlignment.Near, StringAlignment.Near);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _name.Dispose();
                _small.Dispose();
            }
            base.Dispose(disposing);
        }
    }

    /// <summary>Row of pill buttons for picking how much RAM Minecraft gets.</summary>
    internal sealed class RamSelector : PaintedControl
    {
        private readonly int[] _options = { 2, 3, 4, 6, 8 };
        private readonly Font _font = Ui.Font(9f, FontStyle.Bold);
        private readonly int _maxSafe;
        private int _hover = -1;
        public int Value;
        public event EventHandler ValueChanged;

        public RamSelector(int totalGb)
        {
            // Leave a couple of GB for Windows and the launcher.
            _maxSafe = Math.Max(2, totalGb - 2);
            Value = totalGb >= 12 ? 4 : totalGb >= 8 ? 3 : 2;
            Cursor = Cursors.Hand;
        }

        private RectangleF Slot(int i)
        {
            float gap = Ui.S(6);
            float w = (Width - gap * (_options.Length - 1)) / _options.Length;
            return new RectangleF(i * (w + gap), 0.5f, w, Height - 1.5f);
        }

        private int SlotAt(Point p)
        {
            for (int i = 0; i < _options.Length; i++) if (Slot(i).Contains(p)) return i;
            return -1;
        }

        private bool Allowed(int gb)
        {
            return gb <= _maxSafe || gb == 2;
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            base.OnMouseMove(e);
            int i = SlotAt(e.Location);
            if (i != _hover) { _hover = i; Invalidate(); }
        }

        protected override void OnMouseLeave(EventArgs e)
        {
            base.OnMouseLeave(e);
            _hover = -1;
            Invalidate();
        }

        protected override void OnMouseClick(MouseEventArgs e)
        {
            base.OnMouseClick(e);
            int i = SlotAt(e.Location);
            if (!Enabled || i < 0 || !Allowed(_options[i])) return;
            Value = _options[i];
            Invalidate();
            var handler = ValueChanged;
            if (handler != null) handler(this, EventArgs.Empty);
        }

        protected override void OnEnabledChanged(EventArgs e)
        {
            base.OnEnabledChanged(e);
            Invalidate();
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            Ui.Smooth(g);
            for (int i = 0; i < _options.Length; i++)
            {
                int gb = _options[i];
                var r = Slot(i);
                bool selected = gb == Value;
                bool allowed = Allowed(gb);
                using (var path = Ui.RoundRect(r, Ui.S(8)))
                {
                    if (selected)
                    {
                        using (var fill = Ui.AccentBrush(r)) g.FillPath(fill, path);
                    }
                    else
                    {
                        using (var fill = new SolidBrush(i == _hover && allowed && Enabled ? Ui.CardHover : Ui.Card)) g.FillPath(fill, path);
                        using (var pen = new Pen(Ui.Border)) g.DrawPath(pen, path);
                    }
                }
                Color text = selected ? Color.White : allowed ? Ui.Text : Ui.TextFaint;
                Ui.DrawText(g, gb + " GB", _font, text, r, StringAlignment.Center);
            }
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing) _font.Dispose();
            base.Dispose(disposing);
        }
    }

    /// <summary>Rounded button. Primary buttons use the purple-to-blue gradient.</summary>
    internal sealed class FlatButton : PaintedControl
    {
        private bool _hover;
        private bool _down;
        public bool Primary = true;
        private Font _font;

        public FlatButton(float fontSize)
        {
            _font = Ui.Font(fontSize, FontStyle.Bold);
            Cursor = Cursors.Hand;
        }

        protected override void OnMouseEnter(EventArgs e) { base.OnMouseEnter(e); _hover = true; Invalidate(); }

        protected override void OnMouseLeave(EventArgs e) { base.OnMouseLeave(e); _hover = false; _down = false; Invalidate(); }

        protected override void OnMouseDown(MouseEventArgs e) { base.OnMouseDown(e); _down = true; Invalidate(); }

        protected override void OnMouseUp(MouseEventArgs e) { base.OnMouseUp(e); _down = false; Invalidate(); }

        protected override void OnEnabledChanged(EventArgs e) { base.OnEnabledChanged(e); Invalidate(); }

        protected override void OnTextChanged(EventArgs e) { base.OnTextChanged(e); Invalidate(); }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            Ui.Smooth(g);
            var r = new RectangleF(0.5f, 0.5f, Width - 1.5f, Height - 1.5f);
            using (var path = Ui.RoundRect(r, Ui.S(10)))
            {
                if (!Enabled)
                {
                    using (var fill = new SolidBrush(Color.FromArgb(39, 39, 50))) g.FillPath(fill, path);
                }
                else if (Primary)
                {
                    using (var fill = Ui.AccentBrush(r)) g.FillPath(fill, path);
                    if (_hover || _down)
                    {
                        using (var shade = new SolidBrush(_down ? Color.FromArgb(50, 0, 0, 0) : Color.FromArgb(28, 255, 255, 255))) g.FillPath(shade, path);
                    }
                }
                else
                {
                    using (var fill = new SolidBrush(_hover ? Ui.CardHover : Ui.Card)) g.FillPath(fill, path);
                    using (var pen = new Pen(_hover ? Ui.Accent : Ui.Border, Math.Max(1f, Ui.Scale))) g.DrawPath(pen, path);
                }
            }
            Ui.DrawText(g, Text, _font, Enabled ? Color.White : Ui.TextDim, r, StringAlignment.Center);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing) _font.Dispose();
            base.Dispose(disposing);
        }
    }

    /// <summary>Slim rounded progress bar.</summary>
    internal sealed class SlimProgress : PaintedControl
    {
        private double _value;
        public Color Fill = Color.Empty;

        public double Value
        {
            get { return _value; }
            set { _value = Math.Max(0, Math.Min(1, value)); Invalidate(); }
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            Ui.Smooth(g);
            var track = new RectangleF(0, 0, Width - 1, Height - 1);
            using (var path = Ui.RoundRect(track, Height / 2f))
            using (var fill = new SolidBrush(Color.FromArgb(39, 39, 50)))
                g.FillPath(fill, path);
            float w = (float)(track.Width * _value);
            if (w < 1) return;
            var bar = new RectangleF(0, 0, Math.Max(w, Height), track.Height);
            using (var path = Ui.RoundRect(bar, Height / 2f))
            {
                if (Fill != Color.Empty)
                {
                    using (var brush = new SolidBrush(Fill)) g.FillPath(brush, path);
                }
                else
                {
                    using (var brush = Ui.AccentBrush(new RectangleF(0, 0, Width, Height))) g.FillPath(brush, path);
                }
            }
        }
    }

    /// <summary>A small clickable text link (drawn by hand so it looks the same everywhere).</summary>
    internal sealed class TextLink : PaintedControl
    {
        private readonly Font _font = Ui.Font(9f);
        private readonly Font _underline = Ui.Font(9f, FontStyle.Underline);
        private bool _hover;

        public TextLink()
        {
            Cursor = Cursors.Hand;
        }

        protected override void OnMouseEnter(EventArgs e) { base.OnMouseEnter(e); _hover = true; Invalidate(); }

        protected override void OnMouseLeave(EventArgs e) { base.OnMouseLeave(e); _hover = false; Invalidate(); }

        protected override void OnEnabledChanged(EventArgs e) { base.OnEnabledChanged(e); Invalidate(); }

        protected override void OnTextChanged(EventArgs e) { base.OnTextChanged(e); Invalidate(); }

        protected override void OnPaint(PaintEventArgs e)
        {
            var g = e.Graphics;
            Ui.Smooth(g);
            Color color = !Enabled ? Ui.TextFaint : _hover ? Color.White : Color.FromArgb(167, 139, 250);
            Ui.DrawText(g, Text, _hover && Enabled ? _underline : _font, color, new RectangleF(0, 0, Width, Height), StringAlignment.Far);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                _font.Dispose();
                _underline.Dispose();
            }
            base.Dispose(disposing);
        }
    }
}
