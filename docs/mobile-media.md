# Nagrivic Mobile Media & Visual Evidence Foundation

> [!IMPORTANT]
> **Scope Confirmation:**
> Task 27 handles local image capture and selection only.
> Backend media upload is intentionally deferred until issue creation is implemented.

---

## 1. Overview & Architectural Role

Visual civic evidence is central to the Nagrivic platform. Potholes, overflowing garbage containers, broken streetlights, water leakages, and blocked drainage must be documented with clear, unaltered photos.

The mobile media foundation provides a robust, production-grade local capture and gallery selection experience within the report draft flow:

```text
Report Issue
    ↓
Add Photo (Step 1)
    ├── Take Photo
    │      ↓
    │   Camera Permission Check / Request
    │      ↓
    │   Live Camera / System Camera
    │      ↓
    │   Capture & Validate
    │      ↓
    │   Preview in Draft
    │
    └── Choose from Gallery
           ↓
        Gallery Permission Check / Request
           ↓
        Select Photo (Max 1)
           ↓
        Validate (10 MB, Format)
           ↓
        Preview in Draft
           ↓
     Retake / Replace / Remove
           ↓
        Continue to Category (Step 2)
```

The selected photo exists **strictly as local report-draft state** (`DraftMedia`) referencing a local filesystem URI. No network calls, cloud uploads, or fake media IDs are created.

---

## 2. Installed Packages & Expo Compatibility

The mobile project uses **Expo SDK 57** (`~57.0.21`) with React Native 0.86.3 and React 19.2.3. Compatible official Expo packages installed:

| Package | Version | Purpose |
| :--- | :--- | :--- |
| `expo-camera` | `~57.0.5` | Live camera viewfinder (`CameraView`), rear camera default, capture controls |
| `expo-image-picker` | `~57.0.17` | Native gallery image selection, camera launch, permission status checks |

No third-party camera libraries or heavyweight dependencies were added.

---

## 3. Camera Implementation & Lifecycle

- **Component**: `src/components/media/CameraModal.tsx`
- **Default Lens**: Rear camera (`facing="back"`), essential for capturing outdoor civic defects.
- **Switching**: Front/rear flip toggle supported for user convenience.
- **Double-Tap / Shutter Protection**: `isCapturing` lock disables the shutter button and shows an `ActivityIndicator` during capture.
- **Lifecycle Cleanliness**:
  - Modal unmounting releases native camera hardware and stops viewfinder streams.
  - State (`isCapturing`, `cameraError`) resets cleanly whenever modal visibility changes.
  - No background camera services or leaks.

---

## 4. Gallery Picker Implementation

- **Service**: `src/services/media/mediaPickerService.ts` (`pickImageFromGallery`)
- **Single Selection**: `selectionLimit: 1` enforces exactly one civic photo per report draft.
- **User Cancellation**: If the citizen dismisses the picker (`result.canceled === true`), the service returns `{ status: 'CANCELED' }`. Cancellation is treated as a normal user action, not an error.

---

## 5. Permission Handling & Safe Recovery

Permissions follow non-intrusive best practices:
1. **Never on App Launch**: Permissions are never requested at startup.
2. **Explicit User Intent**: Permissions are only checked or requested after the user taps "Take Photo" or "Choose from Gallery".
3. **Safe Denial Explanations**:
   - Camera Denied: *"Camera access is needed to take a photo of the civic issue."*
   - Gallery Denied: *"Photo library access is unavailable."*
4. **Recovery Actions**:
   - Provides **"Try Again"** button when `canAskAgain === true`.
   - Provides **"Open Settings"** (`Linking.openSettings()`) on native devices when permission is blocked/restricted.
5. **No Mutual Blocking**: If Camera permission is denied, Gallery selection remains fully accessible, and vice versa.

---

## 6. Supported Formats & Client Validation

- **Format Constraints**:
  - Supported MIME types: `image/jpeg`, `image/jpg`, `image/png`, `image/webp`.
  - Rejection message: *"This image can't be used. Please choose another photo."*
- **File Size Limit**:
  - Enforces backend limit: **10 MB** (`10,485,760 bytes`).
  - Rejection message: *"Photo is too large. Please choose another image."*
- **Platform Resilience**: If file size is not provided by the platform API, the client does not fabricate a value. It allows the draft to proceed, with backend validation remaining the final authority during future upload.

---

## 7. Camera Quality Settings & Trade-offs

- **Quality Setting**: `0.85` (compression ratio).
- **Rationale**:
  - Captures high-clarity civic evidence (cracks, potholes, garbage volumes, signage) without generating 20+ MB raw images.
  - Typical file sizes range between 1.2 MB and 3.5 MB, well below the 10 MB limit.
  - Balances bandwidth constraints for field reporting across Indian mobile networks.

---

## 8. Draft State Architecture (`ReportDraftContext`)

- **Location**: `src/context/ReportDraftContext.tsx`
- **Lifetime**: In-memory only. Draft state resets on application cold restart.
- **Data Structure**:
  ```typescript
  export interface DraftMedia {
    uri: string;            // Local temporary file URI (e.g. file:///... or blob:...)
    mimeType: string;       // e.g. 'image/jpeg'
    fileSize?: number;      // File size in bytes
    width: number;          // Pixel width
    height: number;         // Pixel height
    source: 'CAMERA' | 'GALLERY';
    fileName?: string | null;
  }
  ```
- **Zero Base64**: No base64 strings or binary blobs are ever stored in React or draft state, preventing mobile memory bloat and UI freezes.

---

## 9. Image Preview, Replacement & Removal

- **Component**: `src/components/media/ImagePreview.tsx`
- **Aspect Ratio**: Uses `resizeMode="cover"` within a framed 280px container with dark slate backing to ensure portrait and landscape photos render without distortion.
- **Actions Supported**:
  - **Retake**: Launches camera to capture a replacement photo.
  - **Choose Another**: Reopens gallery to select a replacement photo.
  - **Remove**: Clears `draft.media` to `null` and returns to the empty upload zone. No server calls or destructive filesystem operations.
  - **Continue**: Confirms photo attachment in draft and advances citizen to issue categorization.

---

## 10. Privacy & EXIF Handling

- `exif: false` is explicitly passed to both `CameraView` and `ImagePicker` options.
- No EXIF GPS coordinates, device serial numbers, or hardware IDs are retained in state or displayed in the UI.
- Municipal boundary matching is handled via authoritative GPS coordinates collected in Step 3, not raw camera EXIF tags.

---

## 11. Accessibility & Design System Alignment

- All touch targets meet or exceed the **44x44px** standard.
- Buttons feature explicit `accessibilityRole="button"` and clear `accessibilityLabel` attributes (`"Take Photo"`, `"Choose from Gallery"`, `"Retake photo using camera"`, `"Remove photo from report draft"`).
- Reuses Nagrivic design tokens (`colors`, `typography`, `spacing`, `radius`, `shadows`) and the custom cross-platform `Icon` component.

---

## 12. Verification & Testing

Automated verification is built into `scripts/verify-media-foundation.js`:
- **Test Suite Command**: `npm run test:media` or `npm test`
- **Coverage**: Validates all 25 acceptance criteria (Tests A through Y) including route structure, button actions, empty states, permission messages, cancellation resilience, format validation, size limit checks, zero base64 verification, API isolation, and TypeScript compilation.
- **TypeScript**: `npx tsc --noEmit` passes with 0 errors.

---

## 13. Future Backend Upload Integration Architecture

When issue creation is implemented in a future task, the complete end-to-end media upload flow will be:

```text
LOCAL MEDIA (Task 27)
    ↓
REPORT DRAFT (In-memory DraftMedia)
    ↓
CITIZEN SUBMITS REPORT
    ↓
ISSUE CREATION (POST /api/issues -> returns issueId)
    ↓
MULTIPART MEDIA UPLOAD (POST /api/issues/{issueId}/media)
    ↓
BACKEND STORAGE & PERSISTENCE (Local/S3 Storage Service)
    ↓
BACKEND MEDIA METADATA (IssueMediaEntity)
```
