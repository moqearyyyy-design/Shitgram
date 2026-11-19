# Shitgram LAN backend

Run `start-server.bat` on the Windows computer. Allow Node.js through Windows Firewall for private networks. Open `http://localhost:8787` on the computer to grant internal test stars.

The mobile app should first call `POST /api/session` with `{ "name": "Nick" }`, retain the returned bearer token, then use:

- `GET /api/messages?after=<last id>` to poll the public LAN chat;
- `POST /api/messages` with `Authorization: Bearer <token>` and `{ "text": "..." }` to post;
- `GET /api/me` to read its internal star balance.
- `POST /api/gifts/send` with `{ "to": "Nick", "title": "Rose", "cost": 10 }` to transfer internal stars and publish a gift message.

This has no connection to Telegram, Telegram Stars, TON, telephone numbers, or Telegram accounts. Before devices other than yours use it, replace the default `SHITGRAM_ADMIN_KEY` in `start-server.bat`.
