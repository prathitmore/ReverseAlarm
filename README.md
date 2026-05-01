# Testing Reverse Alarm

Since this project uses system-level features (Device Admin, Accessibility), you **must** test it on a physical Android device or a fully-featured emulator.

## Prerequisites
1. **Android Studio**: Install the latest version (Hedgehog or newer recommended).
2. **Android Device**: A physical phone is best for testing "locking" behavior.

## How to Run
1. **Open in Android Studio**:
   - Open Android Studio.
   - Select **Open** and navigate to:  
     `C:\Users\Prathit\.gemini\antigravity\scratch\ReverseAlarm`
   - Wait for Gradle Sync to finish. It will download all necessary dependencies.

2. **Prepare Your Phone**:
   - Go to **Settings > About Phone**.
   - Tap **Build Number** 7 times to enable Developer Options.
   - Go to **Settings > System > Developer Options**.
   - Enable **USB Debugging**.

3. **Connect & Install**:
   - Plug your phone into the computer.
   - Click "Allow USB Debugging" on your phone screen if prompted.
   - In Android Studio, select your phone from the device dropdown (top bar).
   - Click the green **Run** (Play) button.

## On the Device (First Run)
The app needs three critical permissions to function. The app is built to guide you, but here is the manual flow if needed:
1. **Display Over Other Apps (Overlay)**:
   - Click "Grant" -> Find "Reverse Alarm" -> Toggle **ON**.
2. **Accessibility Service**:
   - Click "Grant" -> Find "Reverse Alarm Blocker" (it might be under "Installed Apps") -> Toggle **ON** -> Allow.
3. **Device Admin**:
   - Click "Grant" -> Scroll down and tap **Activate this device admin app**.

## Testing the Lock
1. Tap the **Red "TEST LOCK" Button** on the home screen.
2. **Behavior**: The screen will go black with "GO TO SLEEP".
3. **Safety**: For this prototype, the lock **AUTO-DISABLES AFTER 15 SECONDS**. This is to prevent you from getting permanently locked out during testing.
