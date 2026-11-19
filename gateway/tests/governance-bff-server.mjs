import { createServer } from 'node:http';
import { createRequire } from 'node:module';

// Run Next in this owned process, so closing the fixture does not orphan a CLI development worker.
const require = createRequire(process.env.CHAIN_FRONTEND_PACKAGE);
const next = require('next');
const port = Number(process.env.CHAIN_BFF_PORT);
const app = next({ dev: true, dir: process.cwd(), hostname: '127.0.0.1', port });
await app.prepare();
const server = createServer(app.getRequestHandler());
await new Promise((done, reject) => {
  server.once('error', reject);
  server.listen(port, '127.0.0.1', done);
});
async function close() {
  server.closeAllConnections();
  await new Promise((done) => server.close(done));
  await app.close();
  process.exit(0);
}
process.once('SIGTERM', close);
process.once('SIGINT', close);
