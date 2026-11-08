# Automated GitHub Actions APK Build & Free Release Distribution

This plan establishes a zero-cost, automated CI/CD pipeline using **GitHub Actions** to build the **Micoder Studio** APK on every code push or version release. Users and friends can download the installable `.apk` directly to their Android phones via GitHub Releases without paying the $25 Google Play Console fee.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architecture decisions reflect your choices from Phase 1:

- **Free Distribution Channel**: GitHub Releases & Actions artifacts (100% free forever, no Google Play developer account or credit card required).
- **Automation Pipeline**: A dedicated GitHub Actions workflow (`.github/workflows/build-apk.yml`) triggered on `push`, `pull_request`, and manual `workflow_dispatch` button clicks.
- **Direct Phone Download**: The workflow automatically attaches the compiled `MicoderStudio.apk` to a public GitHub Release tag (e.g., `v1.0.0`) providing a direct one-tap download link for mobile browsers.
- **Gradle Wrapper Support**: Ensure the repository has standard Gradle wrapper scripts (`gradlew`, `gradlew.bat`, and `gradle-wrapper.jar`) so GitHub's cloud runners can build the project automatically without pre-installing Gradle.

---

## 1. Overview & Core Concept

- **What It Does**: Whenever you push this project to your GitHub repository (via AI Studio's "Push to GitHub" button), GitHub's free cloud servers will automatically:
  1. Check out your code.
  2. Set up Java 17 (Temurin).
  3. Compile the project with `./gradlew assembleDebug`.
  4. Upload the installable `MicoderStudio.apk` as a downloadable artifact.
  5. Automatically create a public GitHub Release with a direct download link.
- **Target Audience**: Users seeking free distribution, beta testers, and developers without a Play Store console account.
- **Key Value**: Instant, zero-cost, worldwide distribution with zero manual compiling needed on your personal computer or phone.

---

## 2. Step-by-Step User Workflow (Connecting AI Studio to GitHub)

```
┌─────────────────────────┐       ┌─────────────────────────┐       ┌─────────────────────────┐
│   Google AI Studio      │       │     GitHub Repository   │       │   GitHub Releases / DL  │
│  - Click "Export"       │ ───►  │  - Actions Cloud Runner │ ───►  │  - One-tap APK Download │
│  - Push to GitHub Repo  │       │  - Compiles APK (1-2m)  │       │  - Install on Any Phone │
└─────────────────────────┘       └─────────────────────────┘       └─────────────────────────┘
```

### Step 1: Push Repository from AI Studio to GitHub
1. In Google AI Studio, click the **Export** or **GitHub** button in the top menu.
2. Select **Push to GitHub** and connect your GitHub account.
3. Choose a repository name (e.g., `micoder-studio`) and make it **Public**.

### Step 2: Automatic Compilation in the Cloud
1. As soon as code is pushed, GitHub Actions automatically detects `.github/workflows/build-apk.yml`.
2. Go to the **Actions** tab in your GitHub repository.
3. You will see the "Build Micoder Studio APK" workflow running (takes ~2 minutes).

### Step 3: Download & Install on Any Android Phone
1. When the run finishes, open the **Releases** section on the right side of your GitHub repository.
2. Download `MicoderStudio.apk` directly to your phone.
3. Tap the file in your phone's notification bar or Files app, allow "Install from this source", and the studio app installs immediately!

---

## 3. Technical Deliverables to Implement in Repository

1. **`.github/workflows/build-apk.yml`**:
   - Multi-trigger workflow (`push` to main, `tags`, and manual `workflow_dispatch` button).
   - Ubuntu runner with `actions/setup-java@v4` (Java 17).
   - Executes `./gradlew assembleDebug --stacktrace`.
   - Renames output artifact to `MicoderStudio-v1.0.apk`.
   - Uses `actions/upload-artifact@v4` to save the APK in workflow run results.
   - Uses `softprops/action-gh-release@v2` to publish to GitHub Releases whenever a tag or release is pushed.
2. **Gradle Wrapper Verification**:
   - Ensure the Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle-wrapper.jar`) are properly structured in the repository so cloud builds run reliably.
