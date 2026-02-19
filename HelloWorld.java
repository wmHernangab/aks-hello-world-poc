import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

public class HelloWorld {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new RootHandler());
        server.createContext("/healthz", new HealthHandler());

        server.setExecutor(null);
        System.out.println("Listening on http://0.0.0.0:" + port);
        server.start();
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
          try {
              
            String host = Optional.ofNullable(exchange.getRequestHeaders().getFirst("Host")).orElse("unknown");
            String userAgent = Optional.ofNullable(exchange.getRequestHeaders().getFirst("User-Agent")).orElse("unknown");
            int port = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));

            String template = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>AKS HelloWorld</title>
                  <style>
                    :root{
                      --bg0:#05040a;
                      --bg1:#09071a;
                      --neonCyan:#39f6ff;
                      --neonPink:#ff3bf5;
                      --neonLime:#c7ff4a;
                      --grid:#1f1a3a;
                      --text:#e8e6ff;
                      --muted:#a7a3d6;
                      --card: rgba(10, 8, 28, 0.68);
                      --card2: rgba(9, 7, 26, 0.55);
                      --border: rgba(57, 246, 255, 0.35);
                      --shadow: 0 0 24px rgba(57,246,255,.20), 0 0 64px rgba(255,59,245,.10);
                    }

                    *{ box-sizing:border-box; }
                    html,body{ height:100%; }
                    body{
                      margin:0;
                      font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, "Apple Color Emoji","Segoe UI Emoji";
                      color:var(--text);
                      background:
                        radial-gradient(1000px 600px at 70% 10%, rgba(255,59,245,.22), transparent 60%),
                        radial-gradient(900px 500px at 15% 15%, rgba(57,246,255,.18), transparent 55%),
                        linear-gradient(180deg, var(--bg0), var(--bg1));
                      overflow-x:hidden;
                    }

                    /* Retro grid */
                    .grid{
                      position:fixed; inset:0;
                      pointer-events:none;
                      background:
                        linear-gradient(to right, transparent 0 96%, rgba(57,246,255,.10) 98%, transparent 100%),
                        linear-gradient(to bottom, transparent 0 96%, rgba(255,59,245,.10) 98%, transparent 100%);
                      background-size: 70px 70px;
                      opacity:.22;
                      transform: perspective(800px) rotateX(62deg) translateY(180px);
                      transform-origin: top;
                      filter: blur(.2px);
                    }
                    .scanlines{
                      position:fixed; inset:0;
                      pointer-events:none;
                      background: repeating-linear-gradient(
                        to bottom,
                        rgba(255,255,255,.04),
                        rgba(255,255,255,.04) 1px,
                        transparent 1px,
                        transparent 4px
                      );
                      mix-blend-mode: overlay;
                      opacity:.35;
                    }
                    .noise{
                      position:fixed; inset:0;
                      pointer-events:none;
                      background-image:url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="180" height="180"><filter id="n"><feTurbulence type="fractalNoise" baseFrequency=".9" numOctaves="2" stitchTiles="stitch"/></filter><rect width="180" height="180" filter="url(%23n)" opacity=".35"/></svg>');
                      opacity:.07;
                      mix-blend-mode: screen;
                    }

                    .wrap{
                      max-width: 1050px;
                      margin: 0 auto;
                      padding: 36px 18px 56px;
                      position:relative;
                    }

                    header{
                      display:flex;
                      align-items:flex-end;
                      justify-content:space-between;
                      gap:14px;
                      padding: 18px 18px 14px;
                      border:1px solid var(--border);
                      background: linear-gradient(180deg, rgba(10,8,28,.75), rgba(10,8,28,.35));
                      box-shadow: var(--shadow);
                      border-radius: 18px;
                      backdrop-filter: blur(8px);
                    }

                    .brand{
                      display:flex; flex-direction:column; gap:6px;
                    }

                    .title{
                      font-weight: 800;
                      letter-spacing: .06em;
                      text-transform: uppercase;
                      font-size: 18px;
                    }
                    .title span{
                      color: var(--neonCyan);
                      text-shadow: 0 0 12px rgba(57,246,255,.55);
                    }

                    .tag{
                      font-size: 13px;
                      color: var(--muted);
                      line-height: 1.3;
                    }

                    .pill{
                      font-size: 12px;
                      padding: 8px 10px;
                      border-radius: 999px;
                      border: 1px solid rgba(199,255,74,.45);
                      color: rgba(199,255,74,.95);
                      background: rgba(199,255,74,.08);
                      text-shadow: 0 0 10px rgba(199,255,74,.22);
                      white-space:nowrap;
                    }

                    main{
                      margin-top: 18px;
                      display:grid;
                      grid-template-columns: 1.2fr .8fr;
                      gap: 18px;
                    }

                    @media (max-width: 880px){
                      main{ grid-template-columns: 1fr; }
                      header{ align-items:flex-start; }
                    }

                    .card{
                      border-radius: 18px;
                      border: 1px solid rgba(57,246,255,.22);
                      background: linear-gradient(180deg, var(--card), var(--card2));
                      box-shadow: var(--shadow);
                      padding: 16px 16px 14px;
                      backdrop-filter: blur(10px);
                      position:relative;
                      overflow:hidden;
                    }
                    .card:before{
                      content:"";
                      position:absolute; inset:-2px;
                      background: conic-gradient(from 180deg,
                        rgba(57,246,255,.00),
                        rgba(57,246,255,.18),
                        rgba(255,59,245,.16),
                        rgba(199,255,74,.10),
                        rgba(57,246,255,.00)
                      );
                      filter: blur(20px);
                      opacity:.55;
                      pointer-events:none;
                    }
                    .card > *{ position:relative; }

                    h1{
                      margin: 0 0 6px;
                      font-size: 30px;
                      letter-spacing: .02em;
                    }
                    .subtitle{
                      margin: 0 0 12px;
                      color: var(--muted);
                      font-size: 14px;
                      line-height:1.5;
                    }

                    .kv{
                      width:100%;
                      border-collapse: collapse;
                      margin-top: 10px;
                      font-size: 13px;
                    }
                    .kv td{
                      padding: 10px 8px;
                      border-top: 1px solid rgba(255,255,255,.08);
                      vertical-align: top;
                    }
                    .kv td:first-child{
                      color: rgba(57,246,255,.95);
                      width: 160px;
                      font-weight: 600;
                      text-shadow: 0 0 10px rgba(57,246,255,.16);
                    }
                    .mono{
                      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
                      font-size: 12.5px;
                      color: rgba(232,230,255,.92);
                      background: rgba(0,0,0,.25);
                      border: 1px solid rgba(255,255,255,.08);
                      padding: 8px 10px;
                      border-radius: 12px;
                      overflow:auto;
                    }

                    .buttons{
                      display:flex;
                      gap:10px;
                      flex-wrap:wrap;
                      margin-top: 12px;
                    }
                    a.btn{
                      display:inline-flex;
                      align-items:center;
                      gap:8px;
                      padding: 10px 12px;
                      border-radius: 14px;
                      border: 1px solid rgba(255,59,245,.35);
                      background: rgba(255,59,245,.10);
                      color: var(--text);
                      text-decoration:none;
                      font-size: 13px;
                      box-shadow: 0 0 18px rgba(255,59,245,.10);
                    }
                    a.btn:hover{
                      background: rgba(255,59,245,.16);
                      border-color: rgba(255,59,245,.55);
                    }

                    .side h2{
                      margin:0 0 8px;
                      font-size: 14px;
                      letter-spacing:.08em;
                      text-transform: uppercase;
                      color: rgba(199,255,74,.95);
                      text-shadow: 0 0 12px rgba(199,255,74,.18);
                    }
                    .side ul{
                      margin: 10px 0 0;
                      padding-left: 18px;
                      color: rgba(232,230,255,.88);
                      line-height: 1.55;
                      font-size: 13px;
                    }

                    footer{
                      margin-top: 16px;
                      color: rgba(167,163,214,.85);
                      font-size: 12px;
                      text-align:center;
                    }
                  </style>
                </head>
                <body>
                  <div class="grid"></div>
                  <div class="scanlines"></div>
                  <div class="noise"></div>

                  <div class="wrap">
                    <header>
                      <div class="brand">
                        <div class="title"><span>AKS</span> // Hello World</div>
                        <div class="tag">Basic page to validate deployments: Service, Ingress, probes, scaling, and logs.</div>
                      </div>
                      <div class="pill">2026-ready • HelloWorld</div>
                    </header>

                    <main>
                      <section class="card">
                        <h1>Deployment verified</h1>
                        <p class="subtitle">
                          If you're seeing this, your container is responding over HTTP.
                          Use it to check connectivity, load balancing, and observability in AKS.
                        </p>

                        <table class="kv">
                          <tr><td>Timestamp (UTC)</td><td class="mono">%s</td></tr>
                          <tr><td>Host header</td><td class="mono">%s</td></tr>
                          <tr><td>User-Agent</td><td class="mono">%s</td></tr>
                          <tr><td>Health endpoint</td><td class="mono">GET /healthz → 200 ok</td></tr>
                          <tr><td>Port</td><td class="mono">%d (env PORT if set)</td></tr>
                        </table>

                        <div class="buttons">
                          <a class="btn" href="/healthz">Open /healthz</a>
                          <a class="btn" href="/" onclick="location.reload(); return false;">Reload</a>
                        </div>
                      </section>

                      <aside class="card side">
                        <h2>AKS Checklist</h2>
                        <ul>
                          <li>Service (ClusterIP/LoadBalancer) routes to the Pod.</li>
                          <li>Readiness/Liveness: use <span class="mono">/healthz</span>.</li>
                        </ul>

                        <h2 style="margin-top:14px;">Quick tip</h2>
                        <div class="mono">
                          az aks get-credentials --resource-group &lt;resource-group-name&gt; --name &lt;cluster-name&gt; <br/>
                          kubectl get namespaces<br/>
                          kubectl get deploy -n &lt;namespace&gt;<br/>
                          kubectl get pods -n &lt;namespace&gt; <br/>
                          kubectl get svc -n &lt;namespace&gt; <br/>
                          kubectl logs -n deploy/&lt;name&gt; &lt;pod-name&gt; <br/>
                        </div>
                      </aside>
                    </main>

                    <footer>
                      Signal established. If the grid moves… it's probably your imagination (or your Ingress).
                    </footer>
                  </div>
                </body>
                </html>
                """;
              template = template.replace("%s", "@@ARG_S@@").replace("%d", "@@ARG_D@@");
              template = template.replace("%", "%%");
              template = template.replace("@@ARG_S@@", "%s").replace("@@ARG_D@@", "%d");
              String html = template.formatted(Instant.now().toString(), escapeHtml(host), escapeHtml(userAgent), port);

            byte[] body = html.getBytes(StandardCharsets.UTF_8);

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/html; charset=utf-8");
            headers.set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
          } catch (Exception e) {
              e.printStackTrace();
              String errorHtml = """
                  <!doctype html>
                  <html lang="es">
                  <head><meta charset="utf-8"/><title>Error</title></head>
                  <body style="font-family:sans-serif; background:#05040a; color:#e8e6ff; display:flex; align-items:center; justify-content:center; height:100vh;">
                    <div style="text-align:center;">
                      <h1 style="font-size:48px;">¡Ups!</h1>
                      <p style="font-size:18px;">Ocurrió un error inesperado.</p>
                      <pre style="background:#1f1a3a; padding:12px; border-radius:8px; color:#ff3bf5; overflow:auto;">%s</pre>
                    </div>
                  </body>
                  </html>
                  """.formatted(escapeHtml(e.getMessage()));
              byte[] body = errorHtml.getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
              exchange.sendResponseHeaders(500, body.length);
              try (OutputStream os = exchange.getResponseBody()) {
                  os.write(body);
              }
              return;
          }
        }

        private String escapeHtml(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }
}
