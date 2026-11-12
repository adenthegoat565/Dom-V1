using System;
using System.IO;
using System.Net;
using System.Security.Cryptography;
using System.Threading;

namespace DomInstaller
{
    public static class Downloader
    {
        private const string UserAgent = "Dom-V1-Installer/1.0 (+https://github.com/adenthegoat565/Dom-V1)";

        static Downloader()
        {
            // .NET Framework 4.8 normally lets Windows pick TLS 1.2/1.3. Older defaults only had TLS 1.0,
            // which Modrinth and Fabric refuse, so make sure 1.2 is allowed either way.
            if (ServicePointManager.SecurityProtocol != 0)
            {
                ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls12;
            }
            ServicePointManager.DefaultConnectionLimit = Math.Max(ServicePointManager.DefaultConnectionLimit, 8);
        }

        /// <summary>Downloads a file into memory, retrying a few times. progress gets (bytesSoFar, totalBytesOrMinus1).</summary>
        public static byte[] Download(string url, Action<long, long> progress, CancellationToken cancel)
        {
            Exception last = null;
            for (int attempt = 1; attempt <= 4; attempt++)
            {
                cancel.ThrowIfCancellationRequested();
                try
                {
                    return DownloadOnce(url, progress, cancel);
                }
                catch (OperationCanceledException)
                {
                    throw;
                }
                catch (WebException e) when (IsNotFound(e))
                {
                    throw new IOException("File not found on the server: " + url, e);
                }
                catch (Exception e)
                {
                    last = e;
                    if (attempt < 4) Thread.Sleep(1000 * attempt * attempt);
                }
            }
            throw new IOException("Couldn't download " + url + " (" + (last == null ? "unknown error" : last.Message) + ")", last);
        }

        private static bool IsNotFound(WebException e)
        {
            var response = e.Response as HttpWebResponse;
            return response != null && response.StatusCode == HttpStatusCode.NotFound;
        }

        private static byte[] DownloadOnce(string url, Action<long, long> progress, CancellationToken cancel)
        {
            var request = (HttpWebRequest)WebRequest.Create(url);
            request.UserAgent = UserAgent;
            request.Timeout = 30000;
            request.ReadWriteTimeout = 30000;
            request.AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate;
            using (cancel.Register(request.Abort))
            using (var response = (HttpWebResponse)request.GetResponse())
            using (var stream = response.GetResponseStream())
            using (var memory = new MemoryStream(response.ContentLength > 0 ? (int)response.ContentLength : 64 * 1024))
            {
                long total = response.ContentLength;
                var buffer = new byte[64 * 1024];
                int read;
                while ((read = stream.Read(buffer, 0, buffer.Length)) > 0)
                {
                    cancel.ThrowIfCancellationRequested();
                    memory.Write(buffer, 0, read);
                    if (progress != null) progress(memory.Length, total);
                }
                return memory.ToArray();
            }
        }

        public static string Sha512(byte[] data)
        {
            using (var sha = SHA512.Create()) return Hex(sha.ComputeHash(data));
        }

        public static string Sha1(byte[] data)
        {
            using (var sha = SHA1.Create()) return Hex(sha.ComputeHash(data));
        }

        public static string Sha512(string file)
        {
            using (var sha = SHA512.Create())
            using (var stream = File.OpenRead(file)) return Hex(sha.ComputeHash(stream));
        }

        public static string Sha1(string file)
        {
            using (var sha = SHA1.Create())
            using (var stream = File.OpenRead(file)) return Hex(sha.ComputeHash(stream));
        }

        private static string Hex(byte[] hash)
        {
            var chars = new char[hash.Length * 2];
            const string digits = "0123456789abcdef";
            for (int i = 0; i < hash.Length; i++)
            {
                chars[i * 2] = digits[hash[i] >> 4];
                chars[i * 2 + 1] = digits[hash[i] & 0xF];
            }
            return new string(chars);
        }

        /// <summary>Writes a file by writing a temporary copy first, so a crash never leaves half a file behind.</summary>
        public static void WriteAtomic(string path, byte[] data)
        {
            Directory.CreateDirectory(Path.GetDirectoryName(path));
            string temp = path + ".part";
            File.WriteAllBytes(temp, data);
            if (File.Exists(path))
            {
                File.Replace(temp, path, null, true);
            }
            else
            {
                File.Move(temp, path);
            }
        }
    }
}
