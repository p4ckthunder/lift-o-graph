const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");

const port = Number(process.env.PORT || 8790);
const expectedKey = process.env.LIFTOGRAPH_API_KEY || "";
const outputDir = path.join(__dirname, "..", "output");
const outputFile = path.join(outputDir, "api-events.jsonl");

fs.mkdirSync(outputDir, { recursive: true });

const server = http.createServer((request, response) => {
  if (request.method !== "POST" || request.url !== "/liftograph/events") {
    response.writeHead(404, { "content-type": "application/json" });
    response.end(JSON.stringify({ error: "not_found" }));
    return;
  }

  if (expectedKey) {
    const authorization = request.headers.authorization || "";
    if (authorization !== `Bearer ${expectedKey}`) {
      response.writeHead(401, { "content-type": "application/json" });
      response.end(JSON.stringify({ error: "unauthorized" }));
      return;
    }
  }

  let body = "";
  request.setEncoding("utf8");
  request.on("data", (chunk) => {
    body += chunk;
  });
  request.on("end", () => {
    try {
      const event = JSON.parse(body);
      fs.appendFileSync(outputFile, `${JSON.stringify({ receivedAt: new Date().toISOString(), event })}\n`);
      response.writeHead(202, { "content-type": "application/json" });
      response.end(JSON.stringify({ ok: true }));
      console.log("Accepted Lift-O-Graph API event:", event.type, event.exercise?.name);
    } catch (error) {
      response.writeHead(400, { "content-type": "application/json" });
      response.end(JSON.stringify({ error: "invalid_json" }));
    }
  });
});

server.listen(port, "0.0.0.0", () => {
  console.log(`Lift-O-Graph API mock listening on http://0.0.0.0:${port}/liftograph/events`);
  console.log(`Writing events to ${outputFile}`);
});
