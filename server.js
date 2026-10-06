const http = require('http');
const fs = require('fs');
const path = require('path');

const PUBLIC_DIR = '/app/applet/public';
const DB_FILE = path.join(PUBLIC_DIR, 'data/db.json');
const UPLOADS_DIR = path.join(PUBLIC_DIR, 'uploads/videos');
const PORT = 3000;

// Ensure directories exist
fs.mkdirSync(path.join(PUBLIC_DIR, 'data'), { recursive: true });
fs.mkdirSync(UPLOADS_DIR, { recursive: true });

function loadDb() {
  try {
    if (!fs.existsSync(DB_FILE)) {
      return { videos: [], shorts: [], users: {}, comments: {} };
    }
    return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
  } catch (e) {
    console.error('Error loading DB:', e);
    return { videos: [], shorts: [], users: {}, comments: {} };
  }
}

function saveDb(data) {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2), 'utf8');
  } catch (e) {
    console.error('Failed to save DB:', e);
  }
}

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.apk': 'application/vnd.android.package-archive',
  '.aab': 'application/octet-stream',
  '.zip': 'application/zip',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.mp4': 'video/mp4',
  '.wav': 'audio/wav',
  '.mp3': 'audio/mpeg'
};

const server = http.createServer((req, res) => {
  // Common CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-user-handle');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // -------------------------------------------------------------
  // API ROUTING
  // -------------------------------------------------------------

  // 1. GET /api/feed -> Central online source of truth for all users
  if (req.method === 'GET' && pathname === '/api/feed') {
    const db = loadDb();
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({
      success: true,
      videos: db.videos || [],
      shorts: db.shorts || [],
      users: db.users || {},
      comments: db.comments || {}
    }));
    return;
  }

  // 2. POST /api/upload -> Real Cloud Media Upload
  if (req.method === 'POST' && pathname === '/api/upload') {
    const filename = `video_${Date.now()}_${Math.floor(Math.random() * 10000)}.mp4`;
    const targetPath = path.join(UPLOADS_DIR, filename);
    const writeStream = fs.createWriteStream(targetPath);

    req.pipe(writeStream);

    writeStream.on('finish', () => {
      const stats = fs.statSync(targetPath);
      const host = req.headers.host || 'ais-pre-4angobnrimj6sryymqfpgm-149853408349.europe-west2.run.app';
      const proto = req.headers['x-forwarded-proto'] || 'https';
      const fileUrl = `${proto}://${host}/uploads/videos/${filename}`;

      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({
        success: true,
        url: fileUrl,
        relativePath: `/uploads/videos/${filename}`,
        sizeBytes: stats.size,
        filename: filename
      }));
    });

    writeStream.on('error', (err) => {
      console.error('Upload stream error:', err);
      res.writeHead(500, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: false, error: 'Failed to write upload stream' }));
    });
    return;
  }

  // Helper to read JSON request body
  function readJsonBody(callback) {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const json = body ? JSON.parse(body) : {};
        callback(null, json);
      } catch (err) {
        callback(err, null);
      }
    });
  }

  // 3. POST /api/videos -> Add new Short or Video to Cloud Database
  if (req.method === 'POST' && pathname === '/api/videos') {
    readJsonBody((err, videoData) => {
      if (err || !videoData || !videoData.title) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Invalid video payload' }));
        return;
      }

      const db = loadDb();
      videoData.id = videoData.id || `vid_${Date.now()}`;
      videoData.createdAt = Date.now();
      videoData.viewsCount = videoData.viewsCount || 0;
      videoData.likesCount = videoData.likesCount || 0;
      videoData.commentsCount = videoData.commentsCount || 0;
      videoData.category = videoData.category || 'تلاوات';

      if (videoData.isShort) {
        db.shorts = [videoData, ...(db.shorts || [])];
      } else {
        db.videos = [videoData, ...(db.videos || [])];
      }

      // Update user video count in cloud users map
      const authorHandle = videoData.authorHandle;
      if (authorHandle && db.users && db.users[authorHandle]) {
        db.users[authorHandle].videosCount = (db.users[authorHandle].videosCount || 0) + 1;
      }

      saveDb(db);

      res.writeHead(201, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, video: videoData }));
    });
    return;
  }

  // 4. DELETE /api/videos/:id -> Strict Security Check (Owner Only)
  if (req.method === 'DELETE' && pathname.startsWith('/api/videos/')) {
    const videoId = pathname.replace('/api/videos/', '');
    const userHandle = req.headers['x-user-handle'];

    const db = loadDb();
    const existingVideo = (db.videos || []).find(v => v.id === videoId) || (db.shorts || []).find(s => s.id === videoId);

    if (!existingVideo) {
      res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: false, error: 'Video not found' }));
      return;
    }

    // Security Rule: Only owner (or admin) can delete
    if (existingVideo.authorHandle && userHandle && existingVideo.authorHandle !== userHandle && userHandle !== 'admin') {
      res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({
        success: false,
        error: 'Forbidden: You are not authorized to delete another user video. Owner must match.'
      }));
      return;
    }

    // Remove from videos and shorts
    db.videos = (db.videos || []).filter(v => v.id !== videoId);
    db.shorts = (db.shorts || []).filter(s => s.id !== videoId);
    if (db.comments && db.comments[videoId]) {
      delete db.comments[videoId];
    }

    // Try deleting uploaded file if it was hosted locally
    if (existingVideo.videoUrl && existingVideo.videoUrl.includes('/uploads/videos/')) {
      try {
        const localFileName = path.basename(existingVideo.videoUrl);
        const fullLocalPath = path.join(UPLOADS_DIR, localFileName);
        if (fs.existsSync(fullLocalPath)) {
          fs.unlinkSync(fullLocalPath);
        }
      } catch (e) {
        console.error('Failed to unlink video file:', e);
      }
    }

    saveDb(db);

    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ success: true, message: 'Video deleted successfully from cloud database' }));
    return;
  }

  // 5. POST /api/videos/:id/like -> Toggle like in cloud DB
  if (req.method === 'POST' && pathname.match(/^\/api\/videos\/[^\/]+\/like$/)) {
    const videoId = pathname.split('/')[3];
    readJsonBody((err, data) => {
      const isLiked = data.isLiked !== undefined ? data.isLiked : true;
      const db = loadDb();

      let found = false;
      db.videos = (db.videos || []).map(v => {
        if (v.id === videoId) {
          found = true;
          return { ...v, likesCount: Math.max(0, (v.likesCount || 0) + (isLiked ? 1 : -1)) };
        }
        return v;
      });
      db.shorts = (db.shorts || []).map(s => {
        if (s.id === videoId) {
          found = true;
          return { ...s, likesCount: Math.max(0, (s.likesCount || 0) + (isLiked ? 1 : -1)) };
        }
        return s;
      });

      if (found) saveDb(db);
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, found }));
    });
    return;
  }

  // 6. POST /api/videos/:id/comments -> Add comment to cloud DB
  if (req.method === 'POST' && pathname.match(/^\/api\/videos\/[^\/]+\/comments$/)) {
    const videoId = pathname.split('/')[3];
    readJsonBody((err, commentData) => {
      if (!commentData || !commentData.text) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Empty comment text' }));
        return;
      }

      const db = loadDb();
      db.comments = db.comments || {};
      const list = db.comments[videoId] || [];
      const newComment = {
        id: commentData.id || `c_${Date.now()}`,
        contentId: videoId,
        authorName: commentData.authorName || 'مستخدم روحانيات',
        authorHandle: commentData.authorHandle || 'user',
        authorAvatarUrl: commentData.authorAvatarUrl || '',
        text: commentData.text.trim(),
        timeAgo: 'الآن',
        likesCount: 0,
        createdAt: Date.now()
      };
      db.comments[videoId] = [newComment, ...list];

      // Increment comments count on video/short
      db.videos = (db.videos || []).map(v => v.id === videoId ? { ...v, commentsCount: (v.commentsCount || 0) + 1 } : v);
      db.shorts = (db.shorts || []).map(s => s.id === videoId ? { ...s, commentsCount: (s.commentsCount || 0) + 1 } : s);

      saveDb(db);
      res.writeHead(201, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, comment: newComment }));
    });
    return;
  }

  // 7. DELETE /api/videos/:id/comments/:commentId -> Delete user's own comment
  if (req.method === 'DELETE' && pathname.match(/^\/api\/videos\/[^\/]+\/comments\/[^\/]+$/)) {
    const parts = pathname.split('/');
    const videoId = parts[3];
    const commentId = parts[5];
    const userHandle = req.headers['x-user-handle'];

    const db = loadDb();
    db.comments = db.comments || {};
    const list = db.comments[videoId] || [];
    const target = list.find(c => c.id === commentId);

    if (target && userHandle && target.authorHandle !== userHandle && userHandle !== 'admin') {
      res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: false, error: 'Only author can delete this comment' }));
      return;
    }

    db.comments[videoId] = list.filter(c => c.id !== commentId);
    db.videos = (db.videos || []).map(v => v.id === videoId ? { ...v, commentsCount: Math.max(0, (v.commentsCount || 0) - 1) } : v);
    db.shorts = (db.shorts || []).map(s => s.id === videoId ? { ...s, commentsCount: Math.max(0, (s.commentsCount || 0) - 1) } : s);

    saveDb(db);
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // 7.1 POST /api/videos/:id/comments/:commentId/reply -> Add reply to a comment
  if (req.method === 'POST' && pathname.match(/^\/api\/videos\/[^\/]+\/comments\/[^\/]+\/reply$/)) {
    const parts = pathname.split('/');
    const videoId = parts[3];
    const commentId = parts[5];
    readJsonBody((err, replyData) => {
      if (!replyData || !replyData.text) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Empty reply text' }));
        return;
      }

      const db = loadDb();
      db.comments = db.comments || {};
      const list = db.comments[videoId] || [];
      const parent = list.find(c => c.id === commentId);

      if (!parent) {
        res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Parent comment not found' }));
        return;
      }

      parent.replies = parent.replies || [];
      const newReply = {
        id: replyData.id || `rep_${Date.now()}`,
        contentId: videoId,
        authorName: replyData.authorName || 'مستخدم روحانيات',
        authorHandle: replyData.authorHandle || 'user',
        authorAvatarUrl: replyData.authorAvatarUrl || '',
        text: replyData.text.trim(),
        timeAgo: 'الآن',
        likesCount: 0,
        createdAt: Date.now()
      };
      parent.replies.push(newReply);

      // Increment video comments count
      db.videos = (db.videos || []).map(v => v.id === videoId ? { ...v, commentsCount: (v.commentsCount || 0) + 1 } : v);
      db.shorts = (db.shorts || []).map(s => s.id === videoId ? { ...s, commentsCount: (s.commentsCount || 0) + 1 } : s);

      saveDb(db);
      res.writeHead(201, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, reply: newReply }));
    });
    return;
  }

  // 7.2 POST /api/videos/:id/comments/:commentId/like -> Toggle like on comment
  if (req.method === 'POST' && pathname.match(/^\/api\/videos\/[^\/]+\/comments\/[^\/]+\/like$/)) {
    const parts = pathname.split('/');
    const videoId = parts[3];
    const commentId = parts[5];
    readJsonBody((err, data) => {
      const isLiked = data.isLiked !== undefined ? data.isLiked : true;
      const db = loadDb();
      db.comments = db.comments || {};
      const list = db.comments[videoId] || [];
      const target = list.find(c => c.id === commentId);

      if (target) {
        target.likesCount = Math.max(0, (target.likesCount || 0) + (isLiked ? 1 : -1));
        saveDb(db);
      }

      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: !!target, likesCount: target ? target.likesCount : 0 }));
    });
    return;
  }

  // 7.3 PUT /api/videos/:id -> Edit Short or Video (Strict Owner Check)
  if (req.method === 'PUT' && pathname.startsWith('/api/videos/')) {
    const videoId = pathname.replace('/api/videos/', '');
    const userHandle = req.headers['x-user-handle'];

    readJsonBody((err, updateData) => {
      if (!updateData) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Empty update data' }));
        return;
      }

      const db = loadDb();
      let target = (db.videos || []).find(v => v.id === videoId) || (db.shorts || []).find(s => s.id === videoId);

      if (!target) {
        res.writeHead(404, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Video not found' }));
        return;
      }

      if (target.authorHandle && userHandle && target.authorHandle !== userHandle && userHandle !== 'admin') {
        res.writeHead(403, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Forbidden: You can only edit your own video.' }));
        return;
      }

      if (updateData.title) target.title = updateData.title.trim();
      if (updateData.description !== undefined) target.description = updateData.description.trim();
      if (updateData.hashtags) target.hashtags = updateData.hashtags;
      if (updateData.thumbnailUrl) target.thumbnailUrl = updateData.thumbnailUrl;
      if (updateData.category) target.category = updateData.category.trim();
      target.updatedAt = Date.now();

      saveDb(db);
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, video: target }));
    });
    return;
  }

  // 8. POST /api/videos/:id/view -> Increment cloud views count
  if (req.method === 'POST' && pathname.match(/^\/api\/videos\/[^\/]+\/view$/)) {
    const videoId = pathname.split('/')[3];
    const db = loadDb();

    let found = false;
    db.videos = (db.videos || []).map(v => {
      if (v.id === videoId) {
        found = true;
        return { ...v, viewsCount: (v.viewsCount || 0) + 1 };
      }
      return v;
    });
    db.shorts = (db.shorts || []).map(s => {
      if (s.id === videoId) {
        found = true;
        return { ...s, viewsCount: (s.viewsCount || 0) + 1 };
      }
      return s;
    });

    if (found) saveDb(db);
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // 9. POST /api/users -> Register or update profile in cloud
  if (req.method === 'POST' && pathname === '/api/users') {
    readJsonBody((err, userData) => {
      if (!userData || !userData.username) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'Invalid user payload' }));
        return;
      }
      const db = loadDb();
      db.users = db.users || {};
      const existing = db.users[userData.username] || {};
      db.users[userData.username] = { ...existing, ...userData, updatedAt: Date.now() };
      saveDb(db);
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, user: db.users[userData.username] }));
    });
    return;
  }

  // 10. POST /api/auth/login -> User login and account portal
  if (req.method === 'POST' && pathname === '/api/auth/login') {
    readJsonBody((err, body) => {
      if (err || !body || !body.username) {
        res.writeHead(400, { 'Content-Type': 'application/json; charset=utf-8' });
        res.end(JSON.stringify({ success: false, error: 'اسم المستخدم مطلوب' }));
        return;
      }
      const db = loadDb();
      db.users = db.users || {};
      const username = body.username.trim().replace(/^@/, '');
      let user = db.users[username];
      if (!user) {
        user = {
          id: `usr_${Date.now()}`,
          name: body.name || username,
          username: username,
          bio: body.bio || 'مستخدم كريم في تطبيق روحانيات 🌙',
          avatarUrl: body.avatarUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200',
          followersCount: 0,
          followingCount: 0,
          likesCount: 0,
          viewsCount: 0,
          videosCount: 0,
          createdAt: Date.now()
        };
        db.users[username] = user;
        saveDb(db);
      }
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ success: true, user }));
    });
    return;
  }

  // -------------------------------------------------------------
  // STATIC FILE SERVING (/index.html, /rowhaniyat.apk, /uploads/*)
  // -------------------------------------------------------------
  let reqPath = pathname;
  if (reqPath === '/' || reqPath === '') {
    reqPath = '/index.html';
  }

  const filePath = path.join(PUBLIC_DIR, reqPath);
  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('File Not Found');
      return;
    }

    res.writeHead(200, {
      'Content-Type': contentType,
      'Content-Length': stats.size,
      'Content-Disposition': ext === '.apk' || ext === '.aab' || ext === '.zip' ? `attachment; filename="${path.basename(filePath)}"` : 'inline',
      'Access-Control-Allow-Origin': '*'
    });

    const stream = fs.createReadStream(filePath);
    stream.pipe(res);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Server listening on port ${PORT}`);
});

process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err);
});
