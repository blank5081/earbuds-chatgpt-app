/* stella-server/server.js */
import express from "express";
import multer from "multer";
import fs from "fs";
import path from "path";
import fetch from "node-fetch";
import FormData from "form-data";
import dotenv from "dotenv";
import cors from "cors";

dotenv.config();
const app = express();
app.use(cors());

const PORT = process.env.PORT || 3333;
const OPENAI_KEY = process.env.OPENAI_API_KEY;
if (!OPENAI_KEY) {
  console.error("Missing OPENAI_API_KEY in environment");
  process.exit(1);
}

const upload = multer({ dest: "uploads/" });

app.post("/converse", upload.single("file"), async (req, res) => {
  const file = req.file;
  if (!file) return res.status(400).json({ error: "No file uploaded (field 'file')" });

  try {
    const filePath = path.resolve(file.path);

    // 1) Transcribe with OpenAI Whisper
    const transcript = await transcribeAudio(filePath);

    // 2) Ask ChatGPT for a short voice reply
    const replyText = await chatReply(transcript);

    // 3) Synthesize TTS (MP3) using OpenAI TTS endpoint
    const mp3Buffer = await synthesizeSpeech(replyText);

    // 4) return MP3 bytes directly for fastest client playback
    res.set("Content-Type", "audio/mpeg");
    res.send(mp3Buffer);

    // cleanup
    try { fs.unlinkSync(filePath); } catch(_) {}
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message || String(err) });
  }
});

app.get("/", (_req, res) => {
  res.send("Stella server is running.");
});

app.listen(PORT, () => {
  console.log(`Stella server listening on port ${PORT}`);
});

/* ---------- Helper functions ---------- */

async function transcribeAudio(filePath) {
  const url = "https://api.openai.com/v1/audio/transcriptions";
  const form = new FormData();
  form.append("file", fs.createReadStream(filePath));
  form.append("model", "whisper-1");

  const r = await fetch(url, {
    method: "POST",
    headers: { Authorization: `Bearer ${OPENAI_KEY}` },
    body: form
  });

  if (!r.ok) {
    const t = await r.text();
    throw new Error("Transcription error: " + t);
  }
  const j = await r.json();
  return j.text || "";
}

async function chatReply(promptText) {
  const url = "https://api.openai.com/v1/chat/completions";
  const body = {
    model: "gpt-4o-mini",
    messages: [
      { role: "system", content: "You are Stella, a concise voice assistant. Reply in short helpful sentences." },
      { role: "user", content: promptText }
    ],
    max_tokens: 200,
    temperature: 0.5
  };

  const r = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${OPENAI_KEY}`
    },
    body: JSON.stringify(body)
  });

  if (!r.ok) {
    const t = await r.text();
    throw new Error("Chat completion error: " + t);
  }
  const j = await r.json();
  const reply = j.choices?.[0]?.message?.content ?? "";
  return reply;
}

async function synthesizeSpeech(text) {
  const url = "https://api.openai.com/v1/audio/speech";
  const body = {
    model: "gpt-4o-mini-tts",
    voice: "shimmer",
    input: text,
    format: "mp3"
  };

  const r = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${OPENAI_KEY}`
    },
    body: JSON.stringify(body)
  });

  if (!r.ok) {
    const t = await r.text();
    throw new Error("TTS error: " + t);
  }

  const ab = await r.arrayBuffer();
  return Buffer.from(ab);
    }
