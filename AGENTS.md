# AGENTS.md - Notiz Backend Specification (Rust / Axum / PostgreSQL)

This document details the REST API specification, JSON payloads, PostgreSQL database schema, authentication flow, and synchronization protocol required for the **Notiz** Rust/Axum backend.

---

## 1. PostgreSQL Database Schema

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Refresh Tokens Table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Folders Table (Hierarchical)
CREATE TABLE folders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    parent_folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Notes Table
CREATE TABLE notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id UUID REFERENCES folders(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL DEFAULT '',
    content_markdown TEXT NOT NULL DEFAULT '',
    due_date_timestamp BIGINT, -- Epoch milliseconds
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    public_token VARCHAR(64) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Attachments Table (Images stored as BLOB / BYTEA or Object Storage)
CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    note_id UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'image/jpeg',
    image_blob BYTEA, -- Image binary data
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Note Sharing Permissions Table
CREATE TABLE note_shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    note_id UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    shared_with_email VARCHAR(255) NOT NULL,
    permission_role VARCHAR(20) NOT NULL DEFAULT 'READ', -- 'READ' or 'WRITE'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(note_id, shared_with_email)
);
```

---

## 2. Authentication & JWT Specifications

- **Access Token**: Short-lived (e.g. 15-30 minutes) JWT sent in `Authorization: Bearer <access_token>`.
  - Claims: `{ "sub": "<user_id>", "username": "<username>", "email": "<email>", "exp": ... }`
- **Refresh Token**: Long-lived (e.g. 30 days) opaque token or JWT saved in encrypted local storage.
- **Endpoints**:
  - `POST /api/v1/auth/register`
    - Request: `{ "username": "...", "email": "...", "password": "..." }`
    - Response (201): `{ "accessToken": "...", "refreshToken": "...", "user": { "id": "...", "username": "...", "email": "..." } }`
  - `POST /api/v1/auth/login`
    - Request: `{ "username": "...", "password": "..." }`
    - Response (200): `{ "accessToken": "...", "refreshToken": "...", "user": { "id": "...", "username": "...", "email": "..." } }`
  - `POST /api/v1/auth/refresh`
    - Request: `{ "refreshToken": "..." }`
    - Response (200): `{ "accessToken": "...", "refreshToken": "..." }`

---

## 3. Core REST API Endpoints

### Folders
- `GET /api/v1/folders`
- `POST /api/v1/folders`
  - Request: `{ "id": "uuid", "name": "Folder Name", "parentFolderId": "uuid" | null, "updatedAt": 1700000000000 }`
- `PUT /api/v1/folders/{id}`
- `DELETE /api/v1/folders/{id}` (Logical delete / tombstone setting `is_deleted = true`)

### Notes
- `GET /api/v1/notes`
- `POST /api/v1/notes`
  - Request: `{ "id": "uuid", "folderId": "uuid" | null, "title": "Note Title", "contentMarkdown": "# Header\nText", "dueDateTimestamp": 1700000000000 | null, "updatedAt": 1700000000000 }`
- `PUT /api/v1/notes/{id}`
- `DELETE /api/v1/notes/{id}` (Logical delete)

### Attachments
- `POST /api/v1/notes/{noteId}/attachments` (Multipart or Base64 / raw JSON payload `{ "id": "uuid", "fileName": "img.jpg", "mimeType": "image/jpeg", "imageBlobBase64": "..." }`)
- `GET /api/v1/attachments/{id}` (Returns binary or Base64 image payload)
- `DELETE /api/v1/attachments/{id}`

### Sharing & Collaboration
- `POST /api/v1/notes/{noteId}/share`
  - Request: `{ "sharedWithEmail": "colleague@example.com", "permissionRole": "READ" | "WRITE" }`
- `POST /api/v1/notes/{noteId}/public-link`
  - Response: `{ "publicUrl": "https://notiz.app/public/p12345", "token": "p12345" }`
- `GET /api/v1/public/notes/{token}` (Public read-only markdown note render)

---

## 4. Bi-Directional Asynchronous Sync Protocol (LWW + Tombstones)

Endpoint: `POST /api/v1/sync`
Header: `Authorization: Bearer <access_token>`

### Client Push & Pull Payload (`SyncRequest`):
```json
{
  "lastSyncTimestamp": 1700000000000,
  "folderChanges": [
    {
      "id": "e3b0c442-...",
      "name": "Work Notes",
      "parentFolderId": null,
      "isDeleted": false,
      "updatedAt": 1700005000000
    }
  ],
  "noteChanges": [
    {
      "id": "a1b2c3d4-...",
      "folderId": "e3b0c442-...",
      "title": "Meeting Agenda",
      "contentMarkdown": "# Agenda\n- Item 1\n- Item 2",
      "dueDateTimestamp": 1700090000000,
      "isDeleted": false,
      "updatedAt": 1700006000000
    }
  ],
  "attachmentChanges": [
    {
      "id": "f5e4d3c2-...",
      "noteId": "a1b2c3d4-...",
      "fileName": "photo.jpg",
      "mimeType": "image/jpeg",
      "imageBlobBase64": "...",
      "isDeleted": false,
      "updatedAt": 1700006000000
    }
  ]
}
```

### Server Response Payload (`SyncResponse`):
```json
{
  "serverSyncTimestamp": 1700007000000,
  "serverFolderChanges": [...],
  "serverNoteChanges": [...],
  "serverAttachmentChanges": [...]
}
```

### Conflict Resolution Strategy:
- **Last-Write-Wins (LWW)**: Compare `updatedAt` timestamps in milliseconds. The entity with the highest `updatedAt` wins.
- **Tombstones**: Items marked with `isDeleted: true` are saved logically and propagated to clients so all devices delete the local record before server garbage collection.
