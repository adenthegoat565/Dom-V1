using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;

namespace DomInstaller
{
    /// <summary>One mod the installer can download from Modrinth.</summary>
    public sealed class ModEntry
    {
        public string Id;
        public string Name;
        public string Description;
        public string Version;
        public bool Required;
        public bool Default;
        public bool Hidden;
        public string FileName;
        public string Url;
        public string Sha512;
        public string Sha1;
        public long Size;
        public List<string> Requires = new List<string>();
    }

    /// <summary>Everything that was baked into the .exe at build time.</summary>
    public sealed class Payload
    {
        public string GameVersion;
        public string LoaderVersion;
        public List<ModEntry> Mods = new List<ModEntry>();
        public JsonObject FabricProfile;
        public string VersionId;
        public byte[] ProfileIcon;
        public string DomClientFileName;
        public byte[] DomClientJar;

        public static Payload Load()
        {
            var payload = new Payload();
            var modpack = Json.ParseObject(ReadText("payload/modpack.json"));
            payload.GameVersion = modpack.GetString("gameVersion");
            payload.LoaderVersion = modpack.GetString("loaderVersion");
            var mods = modpack["mods"] as List<object> ?? new List<object>();
            foreach (var item in mods.OfType<JsonObject>())
            {
                var mod = new ModEntry
                {
                    Id = item.GetString("id"),
                    Name = item.GetString("name"),
                    Description = item.GetString("description") ?? "",
                    Version = item.GetString("version") ?? "",
                    Required = item["required"] is bool && (bool)item["required"],
                    Default = item["default"] is bool && (bool)item["default"],
                    Hidden = item["hidden"] is bool && (bool)item["hidden"],
                    FileName = item.GetString("fileName"),
                    Url = item.GetString("url"),
                    Sha512 = item.GetString("sha512"),
                    Sha1 = item.GetString("sha1"),
                    Size = item["size"] is JsonNumber ? ((JsonNumber)item["size"]).AsLong() : 0,
                };
                var requires = item["requires"] as List<object>;
                if (requires != null) mod.Requires.AddRange(requires.OfType<string>());
                if (string.IsNullOrEmpty(mod.Id) || string.IsNullOrEmpty(mod.Url) || string.IsNullOrEmpty(mod.FileName))
                    throw new InvalidDataException("modpack.json has an incomplete mod entry");
                if (mod.FileName.IndexOfAny(Path.GetInvalidFileNameChars()) >= 0 || mod.FileName.Contains(".."))
                    throw new InvalidDataException("modpack.json has a bad file name: " + mod.FileName);
                payload.Mods.Add(mod);
            }

            payload.FabricProfile = Json.ParseObject(ReadText("payload/fabric-profile.json"));
            payload.VersionId = payload.FabricProfile.GetString("id");
            if (string.IsNullOrEmpty(payload.VersionId)) throw new InvalidDataException("fabric-profile.json has no id");

            payload.ProfileIcon = ReadBytes("payload/profile-icon.png");

            var assembly = Assembly.GetExecutingAssembly();
            string jarResource = assembly.GetManifestResourceNames()
                .FirstOrDefault(n => n.StartsWith("payload/dom-client", StringComparison.Ordinal) && n.EndsWith(".jar", StringComparison.Ordinal));
            if (jarResource == null) throw new InvalidDataException("The Dom Client mod jar is missing from this installer build");
            payload.DomClientFileName = jarResource.Substring("payload/".Length);
            payload.DomClientJar = ReadBytes(jarResource);
            return payload;
        }

        /// <summary>The mod ids that end up installed: required ones, the ones the user ticked, and everything those need.</summary>
        public HashSet<string> Resolve(IEnumerable<string> selected)
        {
            var byId = Mods.ToDictionary(m => m.Id, StringComparer.Ordinal);
            var result = new HashSet<string>(StringComparer.Ordinal);
            var queue = new Queue<string>(Mods.Where(m => m.Required).Select(m => m.Id).Concat(selected ?? Enumerable.Empty<string>()));
            while (queue.Count > 0)
            {
                string id = queue.Dequeue();
                ModEntry mod;
                if (!byId.TryGetValue(id, out mod) || !result.Add(id)) continue;
                foreach (var dep in mod.Requires) queue.Enqueue(dep);
            }
            return result;
        }

        public static Stream Open(string name)
        {
            var stream = Assembly.GetExecutingAssembly().GetManifestResourceStream(name);
            if (stream == null) throw new InvalidDataException("Missing embedded file: " + name);
            return stream;
        }

        public static byte[] ReadBytes(string name)
        {
            using (var stream = Open(name))
            using (var memory = new MemoryStream())
            {
                stream.CopyTo(memory);
                return memory.ToArray();
            }
        }

        public static string ReadText(string name)
        {
            return Encoding.UTF8.GetString(ReadBytes(name));
        }
    }
}
