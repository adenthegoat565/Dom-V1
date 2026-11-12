using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Text;

namespace DomInstaller
{
    /// <summary>A JSON object that keeps its keys in the order they were read, so rewritten files stay tidy.</summary>
    public sealed class JsonObject : IEnumerable<KeyValuePair<string, object>>
    {
        private readonly List<string> _order = new List<string>();
        private readonly Dictionary<string, object> _values = new Dictionary<string, object>(StringComparer.Ordinal);

        public object this[string key]
        {
            get { object value; return _values.TryGetValue(key, out value) ? value : null; }
            set
            {
                if (!_values.ContainsKey(key)) _order.Add(key);
                _values[key] = value;
            }
        }

        public bool Has(string key) { return _values.ContainsKey(key); }

        public int Count { get { return _order.Count; } }

        public bool Remove(string key)
        {
            if (!_values.Remove(key)) return false;
            _order.Remove(key);
            return true;
        }

        public string GetString(string key)
        {
            return this[key] as string;
        }

        public JsonObject GetObject(string key)
        {
            return this[key] as JsonObject;
        }

        public IEnumerator<KeyValuePair<string, object>> GetEnumerator()
        {
            foreach (var key in _order) yield return new KeyValuePair<string, object>(key, _values[key]);
        }

        IEnumerator IEnumerable.GetEnumerator() { return GetEnumerator(); }
    }

    /// <summary>A JSON number, kept as its original text so it is written back exactly.</summary>
    public sealed class JsonNumber
    {
        public readonly string Raw;

        public JsonNumber(string raw) { Raw = raw; }

        public JsonNumber(long value) { Raw = value.ToString(CultureInfo.InvariantCulture); }

        public long AsLong()
        {
            double d;
            return double.TryParse(Raw, NumberStyles.Float, CultureInfo.InvariantCulture, out d) ? (long)d : 0;
        }

        public override string ToString() { return Raw; }
    }

    public sealed class JsonException : Exception
    {
        public JsonException(string message) : base(message) { }
    }

    /// <summary>Small, strict JSON reader and writer (objects, arrays, strings, numbers, true/false/null).</summary>
    public static class Json
    {
        public static object Parse(string text)
        {
            var parser = new Parser(text);
            parser.SkipWhitespace();
            object value = parser.ReadValue(0);
            parser.SkipWhitespace();
            if (!parser.AtEnd) throw new JsonException("Unexpected text after the JSON value at position " + parser.Position);
            return value;
        }

        public static JsonObject ParseObject(string text)
        {
            var obj = Parse(text) as JsonObject;
            if (obj == null) throw new JsonException("Expected a JSON object");
            return obj;
        }

        public static string Write(object value)
        {
            var sb = new StringBuilder();
            WriteValue(sb, value, 0);
            sb.Append('\n');
            return sb.ToString();
        }

        private static void Indent(StringBuilder sb, int depth)
        {
            sb.Append(' ', depth * 2);
        }

        private static void WriteValue(StringBuilder sb, object value, int depth)
        {
            if (value == null) { sb.Append("null"); return; }
            var s = value as string;
            if (s != null) { WriteString(sb, s); return; }
            if (value is bool) { sb.Append((bool)value ? "true" : "false"); return; }
            var n = value as JsonNumber;
            if (n != null) { sb.Append(n.Raw); return; }
            if (value is int || value is long)
            {
                sb.Append(Convert.ToInt64(value).ToString(CultureInfo.InvariantCulture));
                return;
            }
            if (value is double || value is float)
            {
                sb.Append(Convert.ToDouble(value).ToString("R", CultureInfo.InvariantCulture));
                return;
            }
            var obj = value as JsonObject;
            if (obj != null)
            {
                if (obj.Count == 0) { sb.Append("{}"); return; }
                sb.Append("{\n");
                int i = 0;
                foreach (var pair in obj)
                {
                    Indent(sb, depth + 1);
                    WriteString(sb, pair.Key);
                    sb.Append(" : ");
                    WriteValue(sb, pair.Value, depth + 1);
                    if (++i < obj.Count) sb.Append(',');
                    sb.Append('\n');
                }
                Indent(sb, depth);
                sb.Append('}');
                return;
            }
            var list = value as List<object>;
            if (list != null)
            {
                if (list.Count == 0) { sb.Append("[]"); return; }
                sb.Append("[\n");
                for (int i = 0; i < list.Count; i++)
                {
                    Indent(sb, depth + 1);
                    WriteValue(sb, list[i], depth + 1);
                    if (i + 1 < list.Count) sb.Append(',');
                    sb.Append('\n');
                }
                Indent(sb, depth);
                sb.Append(']');
                return;
            }
            throw new JsonException("Can't write a value of type " + value.GetType().Name);
        }

        private static void WriteString(StringBuilder sb, string s)
        {
            sb.Append('"');
            foreach (char c in s)
            {
                switch (c)
                {
                    case '"': sb.Append("\\\""); break;
                    case '\\': sb.Append("\\\\"); break;
                    case '\n': sb.Append("\\n"); break;
                    case '\r': sb.Append("\\r"); break;
                    case '\t': sb.Append("\\t"); break;
                    case '\b': sb.Append("\\b"); break;
                    case '\f': sb.Append("\\f"); break;
                    default:
                        if (c < 0x20) sb.Append("\\u").Append(((int)c).ToString("x4", CultureInfo.InvariantCulture));
                        else sb.Append(c);
                        break;
                }
            }
            sb.Append('"');
        }

        private sealed class Parser
        {
            private const int MaxDepth = 256;
            private readonly string _text;
            private int _pos;

            public Parser(string text)
            {
                _text = text ?? "";
                // Some editors save a byte order mark at the start of the file.
                if (_text.Length > 0 && _text[0] == '﻿') _pos = 1;
            }

            public bool AtEnd { get { return _pos >= _text.Length; } }

            public int Position { get { return _pos; } }

            public void SkipWhitespace()
            {
                while (_pos < _text.Length)
                {
                    char c = _text[_pos];
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r') _pos++;
                    else break;
                }
            }

            private JsonException Error(string message)
            {
                return new JsonException(message + " at position " + _pos);
            }

            private char Peek()
            {
                if (_pos >= _text.Length) throw Error("Unexpected end of JSON");
                return _text[_pos];
            }

            private void Expect(char c)
            {
                if (Peek() != c) throw Error("Expected '" + c + "'");
                _pos++;
            }

            public object ReadValue(int depth)
            {
                if (depth > MaxDepth) throw Error("JSON nested too deeply");
                char c = Peek();
                switch (c)
                {
                    case '{': return ReadObject(depth);
                    case '[': return ReadArray(depth);
                    case '"': return ReadString();
                    case 't': ReadLiteral("true"); return true;
                    case 'f': ReadLiteral("false"); return false;
                    case 'n': ReadLiteral("null"); return null;
                    default:
                        if (c == '-' || (c >= '0' && c <= '9')) return ReadNumber();
                        throw Error("Unexpected character '" + c + "'");
                }
            }

            private void ReadLiteral(string literal)
            {
                if (string.CompareOrdinal(_text, _pos, literal, 0, literal.Length) != 0) throw Error("Expected " + literal);
                _pos += literal.Length;
            }

            private JsonObject ReadObject(int depth)
            {
                Expect('{');
                var obj = new JsonObject();
                SkipWhitespace();
                if (Peek() == '}') { _pos++; return obj; }
                while (true)
                {
                    SkipWhitespace();
                    string key = ReadString();
                    SkipWhitespace();
                    Expect(':');
                    SkipWhitespace();
                    obj[key] = ReadValue(depth + 1);
                    SkipWhitespace();
                    char c = Peek();
                    _pos++;
                    if (c == '}') return obj;
                    if (c != ',') { _pos--; throw Error("Expected ',' or '}'"); }
                }
            }

            private List<object> ReadArray(int depth)
            {
                Expect('[');
                var list = new List<object>();
                SkipWhitespace();
                if (Peek() == ']') { _pos++; return list; }
                while (true)
                {
                    SkipWhitespace();
                    list.Add(ReadValue(depth + 1));
                    SkipWhitespace();
                    char c = Peek();
                    _pos++;
                    if (c == ']') return list;
                    if (c != ',') { _pos--; throw Error("Expected ',' or ']'"); }
                }
            }

            private string ReadString()
            {
                Expect('"');
                var sb = new StringBuilder();
                while (true)
                {
                    if (_pos >= _text.Length) throw Error("Unterminated string");
                    char c = _text[_pos++];
                    if (c == '"') return sb.ToString();
                    if (c != '\\')
                    {
                        sb.Append(c);
                        continue;
                    }
                    if (_pos >= _text.Length) throw Error("Unterminated escape");
                    char e = _text[_pos++];
                    switch (e)
                    {
                        case '"': sb.Append('"'); break;
                        case '\\': sb.Append('\\'); break;
                        case '/': sb.Append('/'); break;
                        case 'b': sb.Append('\b'); break;
                        case 'f': sb.Append('\f'); break;
                        case 'n': sb.Append('\n'); break;
                        case 'r': sb.Append('\r'); break;
                        case 't': sb.Append('\t'); break;
                        case 'u':
                            if (_pos + 4 > _text.Length) throw Error("Bad \\u escape");
                            int code;
                            if (!int.TryParse(_text.Substring(_pos, 4), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out code))
                                throw Error("Bad \\u escape");
                            sb.Append((char)code);
                            _pos += 4;
                            break;
                        default: throw Error("Bad escape '\\" + e + "'");
                    }
                }
            }

            private JsonNumber ReadNumber()
            {
                int start = _pos;
                if (_text[_pos] == '-') _pos++;
                while (_pos < _text.Length)
                {
                    char c = _text[_pos];
                    if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') _pos++;
                    else break;
                }
                string raw = _text.Substring(start, _pos - start);
                double ignored;
                if (!double.TryParse(raw, NumberStyles.Float, CultureInfo.InvariantCulture, out ignored)) throw Error("Bad number '" + raw + "'");
                return new JsonNumber(raw);
            }
        }
    }
}
