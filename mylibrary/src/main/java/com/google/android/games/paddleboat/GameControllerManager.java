//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.google.android.games.paddleboat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.BatteryState;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.hardware.lights.Light;
import android.hardware.lights.LightsManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameControllerManager {
    public static final int MAX_GAMECONTROLLERS = 8;
    public static final int MAX_MICE = 2;
    public static final int VIBRATION_EFFECT_MIN_API = 26;
    public static final int DEVICEFLAG_BATTERY = 67108864;
    public static final int DEVICEFLAG_ACCELEROMETER = 4194304;
    public static final int DEVICEFLAG_GYROSCOPE = 8388608;
    public static final int DEVICEFLAG_LIGHT_PLAYER = 16777216;
    public static final int DEVICEFLAG_LIGHT_RGB = 33554432;
    public static final int DEVICEFLAG_VIBRATION = 134217728;
    public static final int DEVICEFLAG_VIBRATION_DUAL_MOTOR = 268435456;
    public static final int DEVICEFLAG_VIRTUAL_MOUSE = 1073741824;
    public static final int LIGHT_TYPE_PLAYER = 0;
    public static final int LIGHT_TYPE_RGB = 1;
    public static final int MOTION_ACCELEROMETER = 0;
    public static final int MOTION_GYROSCOPE = 1;
    private static final int VIBRATOR_MANAGER_MIN_API = 31;
    private static final String FINGERPRINT_DEVICE_NAME = "uinput-fpc";
    private static final String TAG = "GameControllerManager";
    private static final int GAMECONTROLLER_SOURCE_MASK = 16778769;
    private static final int MOUSE_SOURCE_MASK = 8194;
    private boolean nativeReady;
    private final boolean printControllerInfo;
    private boolean reportMotionEvents;
    private final InputManager inputManager;
    private final ArrayList<Integer> mouseDeviceIds;
    private final ArrayList<Integer> pendingControllerDeviceIds;
    private final ArrayList<Integer> pendingMouseDeviceIds;
    private final ArrayList<GameControllerInfo> gameControllers;
    private GameControllerThread gameControllerThread;

    public GameControllerManager(Context appContext, boolean appPrintControllerInfo) {
        if (appPrintControllerInfo) {
            Log.d("GameControllerManager", "device Info:\n  BRAND: " + Build.BRAND + "\n DEVICE: " + Build.DEVICE + "\n  MANUF: " + Build.MANUFACTURER + "\n  MODEL: " + Build.MODEL + "\nPRODUCT: " + Build.PRODUCT + "\n    API: " + VERSION.SDK_INT);
        }

        this.nativeReady = false;
        this.reportMotionEvents = false;
        this.inputManager = (InputManager)appContext.getSystemService("input");
        this.printControllerInfo = appPrintControllerInfo;
        this.mouseDeviceIds = new ArrayList(8);
        this.pendingControllerDeviceIds = new ArrayList(8);
        this.pendingMouseDeviceIds = new ArrayList(8);
        this.gameControllers = new ArrayList(8);
        this.scanDevices();
    }

    public static int getControllerFlagsForDevice(InputDevice inputDevice) {
        int controllerFlags = 0;
        boolean hasVirtualMouse = isDeviceOfSource(inputDevice.getId(), 8194);
        if (hasVirtualMouse) {
            controllerFlags |= 1073741824;
        }

        int vibratorCount = getVibratorCount(inputDevice);
        if (vibratorCount > 0) {
            controllerFlags |= 134217728;
            if (vibratorCount > 1) {
                controllerFlags |= 268435456;
            }
        }

        if (VERSION.SDK_INT >= 31) {
            SensorManager sensorManager = inputDevice.getSensorManager();
            if (sensorManager != null) {
                if (sensorManager.getSensorList(1).size() > 0) {
                    controllerFlags |= 4194304;
                }

                if (sensorManager.getSensorList(4).size() > 0) {
                    controllerFlags |= 8388608;
                }
            }

            LightsManager lightsManager = inputDevice.getLightsManager();
            if (lightsManager != null) {
                Iterator var6 = lightsManager.getLights().iterator();

                while(var6.hasNext()) {
                    Light currentLight = (Light)var6.next();
                    if (currentLight.getType() == 10002) {
                        controllerFlags |= 16777216;
                    } else if (currentLight.hasRgbControl()) {
                        controllerFlags |= 33554432;
                    }
                }
            }

            BatteryState batteryState = inputDevice.getBatteryState();
            if (batteryState != null && batteryState.isPresent()) {
                controllerFlags |= 67108864;
            }
        }

        return controllerFlags;
    }

    public static int getVibratorCount(InputDevice inputDevice) {
        if (inputDevice != null) {
            if (VERSION.SDK_INT >= 31) {
                VibratorManager vibratorManager = inputDevice.getVibratorManager();
                if (vibratorManager != null) {
                    int[] vibratorIds = vibratorManager.getVibratorIds();
                    int vibratorCount = vibratorIds.length;
                    if (vibratorCount > 0) {
                        return vibratorCount;
                    }
                }
            } else if (VERSION.SDK_INT >= 26) {
                Vibrator deviceVibrator = inputDevice.getVibrator();
                if (deviceVibrator != null && deviceVibrator.hasVibrator()) {
                    return 1;
                }
            }
        }

        return 0;
    }

    private static boolean isDeviceOfSource(int deviceId, int matchingSourceMask) {
        try {
            boolean isSource = false;
            InputDevice inputDevice = InputDevice.getDevice(deviceId);
            int inputDeviceSources = inputDevice.getSources();
            int sourceMask = -256 & matchingSourceMask;
            if (!inputDevice.isVirtual() && (inputDeviceSources & sourceMask) != 0) {
                List<MotionRange> motionRanges = inputDevice.getMotionRanges();
                if (motionRanges.size() > 0) {
                    isSource = true;
                }
            }
            return isSource;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean getPrintControllerInfo() {
        return this.printControllerInfo;
    }

    public InputManager getAppInputManager() {
        return this.inputManager;
    }

    public void onStop() {
        if (this.gameControllerThread != null) {
            this.gameControllerThread.onStop();
        }

    }

    public void onStart() {
        if (this.gameControllerThread != null) {
            this.scanDevices();
            this.gameControllerThread.onStart();
        } else {
            this.gameControllerThread = new GameControllerThread();
            this.gameControllerThread.setGameControllerManager(this);
            this.gameControllerThread.start();
        }

    }

    void checkForControllerRemovals(int[] deviceIds) {
        int index;
        boolean foundDevice;
        int[] var4;
        int var5;
        int var6;
        int deviceId;
        if (!this.nativeReady) {
            for(index = 0; index < this.pendingControllerDeviceIds.size(); ++index) {
                foundDevice = false;
                var4 = deviceIds;
                var5 = deviceIds.length;

                for(var6 = 0; var6 < var5; ++var6) {
                    deviceId = var4[var6];
                    if ((Integer)this.pendingControllerDeviceIds.get(index) == deviceId) {
                        foundDevice = true;
                        break;
                    }
                }

                if (!foundDevice) {
                    this.pendingControllerDeviceIds.remove(index--);
                }
            }
        }

        for(index = 0; index < this.gameControllers.size(); ++index) {
            foundDevice = false;
            var4 = deviceIds;
            var5 = deviceIds.length;

            for(var6 = 0; var6 < var5; ++var6) {
                deviceId = var4[var6];
                if (((GameControllerInfo)this.gameControllers.get(index)).GetGameControllerDeviceId() == deviceId) {
                    foundDevice = true;
                    break;
                }
            }

            if (!foundDevice) {
                this.onInputDeviceRemoved(((GameControllerInfo)this.gameControllers.get(index)).GetGameControllerDeviceId());
            }
        }

    }

    void checkForMouseRemovals(int[] deviceIds) {
        int index;
        int var6;
        int deviceId;
        if (!this.nativeReady) {
            for(index = 0; index < this.pendingMouseDeviceIds.size(); ++index) {
                boolean foundDevice = false;
                int[] var4 = deviceIds;
                int var5 = deviceIds.length;

                for(var6 = 0; var6 < var5; ++var6) {
                    deviceId = var4[var6];
                    if ((Integer)this.pendingMouseDeviceIds.get(index) == deviceId) {
                        foundDevice = true;
                        break;
                    }
                }

                if (!foundDevice) {
                    this.pendingMouseDeviceIds.remove(index--);
                }
            }
        }

        for(index = 0; index < this.mouseDeviceIds.size(); ++index) {
            int mouseDeviceId = (Integer)this.mouseDeviceIds.get(index);
            boolean foundDevice = false;
            int[] var11 = deviceIds;
            var6 = deviceIds.length;

            for(int i = 0; i < var6; ++i) {
                int temp = var11[i];
                if (mouseDeviceId == temp) {
                    foundDevice = true;
                    break;
                }
            }

            if (!foundDevice) {
                this.onInputDeviceRemoved(mouseDeviceId);
            }
        }

    }

    void processControllerAddition(int deviceId) {
        boolean foundDevice = false;
        int index;
        if (!this.nativeReady) {
            for(index = 0; index < this.pendingControllerDeviceIds.size(); ++index) {
                if ((Integer)this.pendingControllerDeviceIds.get(index) == deviceId) {
                    foundDevice = true;
                    break;
                }
            }

            if (!foundDevice) {
                this.pendingControllerDeviceIds.add(deviceId);
            }
        } else {
            for(index = 0; index < this.gameControllers.size(); ++index) {
                if (((GameControllerInfo)this.gameControllers.get(index)).GetGameControllerDeviceId() == deviceId) {
                    foundDevice = true;
                    break;
                }
            }

            if (!foundDevice) {
                this.onGameControllerAdded(deviceId);
            }
        }

    }

    void processMouseAddition(int deviceId) {
        boolean foundDevice = false;
        int index;
        if (!this.nativeReady) {
            for(index = 0; index < this.pendingMouseDeviceIds.size(); ++index) {
                if ((Integer)this.pendingMouseDeviceIds.get(index) == deviceId) {
                    foundDevice = true;
                    break;
                }
            }

            if (!foundDevice) {
                this.pendingMouseDeviceIds.add(deviceId);
            }
        } else {
            for(index = 0; index < this.mouseDeviceIds.size(); ++index) {
                if ((Integer)this.mouseDeviceIds.get(index) == deviceId) {
                    foundDevice = true;
                    break;
                }
            }

            if (!foundDevice) {
                this.onMouseAdded(deviceId);
            }
        }

    }

    boolean getIsGameController(int deviceId) {
        boolean isGameController = false;
        if (isDeviceOfSource(deviceId, 16778769)) {
            InputDevice inputDevice = InputDevice.getDevice(deviceId);
            if (inputDevice != null) {
                String deviceName = inputDevice.getName();
                if (!deviceName.equalsIgnoreCase("uinput-fpc")) {
                    isGameController = true;
                }
            }
        }

        return isGameController;
    }

    void scanDevices() {
        int[] deviceIds = this.inputManager.getInputDeviceIds();
        int[] var2 = deviceIds;
        int var3 = deviceIds.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            int deviceId = var2[var4];
            boolean isGameController = this.getIsGameController(deviceId);
            boolean isMouse = isDeviceOfSource(deviceId, 8194);
            if (isMouse && !isGameController) {
                this.processMouseAddition(deviceId);
            } else if (isGameController) {
                this.processControllerAddition(deviceId);
            }
        }

        this.checkForControllerRemovals(deviceIds);
        this.checkForMouseRemovals(deviceIds);
    }

    GameControllerInfo onGameControllerAdded(int deviceId) {
        GameControllerInfo gameControllerInfo = null;
        if (this.gameControllers.size() < 8) {
            if (this.printControllerInfo) {
                Log.d("GameControllerManager", "onGameControllerDeviceAdded");
                this.logControllerInfo(deviceId);
            }

            InputDevice newDevice = InputDevice.getDevice(deviceId);
            gameControllerInfo = new GameControllerInfo(newDevice);
            GameControllerListener gameControllerListener = new GameControllerListener(this, newDevice, gameControllerInfo.GetGameControllerFlags(), this.reportMotionEvents);
            gameControllerInfo.SetListener(gameControllerListener);
            this.gameControllers.add(gameControllerInfo);
            this.notifyNativeConnection(gameControllerInfo);
        }

        return gameControllerInfo;
    }

    void onMouseAdded(int deviceId) {
        if (this.mouseDeviceIds.size() < 2) {
            if (this.printControllerInfo) {
                Log.d("GameControllerManager", "onMouseDeviceAdded id: " + deviceId + " name: " + InputDevice.getDevice(deviceId).getName());
                this.logControllerInfo(deviceId);
            }

            this.mouseDeviceIds.add(deviceId);
            this.onMouseConnected(deviceId);
        }

    }

    void onGameControllerDeviceRemoved(int deviceId) {
        int index;
        for(index = 0; index < this.pendingControllerDeviceIds.size(); ++index) {
            if ((Integer)this.pendingControllerDeviceIds.get(index) == deviceId) {
                this.pendingControllerDeviceIds.remove(index);
                break;
            }
        }

        for(index = 0; index < this.gameControllers.size(); ++index) {
            GameControllerInfo controller = (GameControllerInfo)this.gameControllers.get(index);
            if (controller.GetGameControllerDeviceId() == deviceId) {
                if (this.nativeReady) {
                    this.onControllerDisconnected(deviceId);
                }

                controller.GetListener().shutdownListener();
                controller.SetListener((GameControllerListener)null);
                this.gameControllers.remove(index);
                break;
            }
        }

    }

    boolean onMouseDeviceRemoved(int deviceId) {
        boolean removed = false;

        int index;
        for(index = 0; index < this.pendingMouseDeviceIds.size(); ++index) {
            if ((Integer)this.pendingMouseDeviceIds.get(index) == deviceId) {
                this.pendingMouseDeviceIds.remove(index);
                removed = true;
                break;
            }
        }

        for(index = 0; index < this.mouseDeviceIds.size(); ++index) {
            if ((Integer)this.mouseDeviceIds.get(index) == deviceId) {
                if (this.nativeReady) {
                    this.onMouseDisconnected(deviceId);
                }

                this.mouseDeviceIds.remove(index);
                removed = true;
                break;
            }
        }

        return removed;
    }

    public void onInputDeviceAdded(int deviceId) {
        boolean isGameController = this.getIsGameController(deviceId);
        boolean isMouse = isDeviceOfSource(deviceId, 8194);
        if (isMouse && !isGameController) {
            this.processMouseAddition(deviceId);
        } else if (isGameController) {
            this.processControllerAddition(deviceId);
        }

    }

    public void onInputDeviceRemoved(int deviceId) {
        this.onMouseDeviceRemoved(deviceId);
        this.onGameControllerDeviceRemoved(deviceId);
    }

    public void onInputDeviceChanged(int deviceId) {
        boolean isGameController = this.getIsGameController(deviceId);
        if (isGameController) {
            boolean foundDeviceId = false;
            Iterator var4 = this.pendingControllerDeviceIds.iterator();

            while(var4.hasNext()) {
                int pendingDeviceId = (Integer)var4.next();
                if (pendingDeviceId == deviceId) {
                    foundDeviceId = true;
                    break;
                }
            }

            if (!foundDeviceId) {
                for(int index = 0; index < this.gameControllers.size(); ++index) {
                    GameControllerInfo controller = (GameControllerInfo)this.gameControllers.get(index);
                    if (controller.GetGameControllerDeviceId() == deviceId) {
                        foundDeviceId = true;
                        InputDevice inputDevice = this.inputManager.getInputDevice(deviceId);
                        int controllerFlags = controller.GetGameControllerFlags();
                        controller.GetListener().resetListener(inputDevice, controllerFlags);
                        break;
                    }
                }
            }

            if (!foundDeviceId) {
                this.processControllerAddition(deviceId);
            }
        }

    }

    public int getApiLevel() {
        return VERSION.SDK_INT;
    }

    public void setNativeReady() {
        this.nativeReady = true;
        Log.d("GameControllerManager", "setNativeReady");
        Iterator var1 = this.pendingControllerDeviceIds.iterator();

        int deviceId;
        while(var1.hasNext()) {
            deviceId = (Integer)var1.next();
            GameControllerInfo gcInfo = this.onGameControllerAdded(deviceId);
            if (gcInfo != null && this.printControllerInfo) {
                Log.d("GameControllerManager", "setNativeReady notifyNativeConnection for deviceId: " + deviceId);
            }
        }

        this.pendingControllerDeviceIds.clear();
        var1 = this.pendingMouseDeviceIds.iterator();

        while(var1.hasNext()) {
            deviceId = (Integer)var1.next();
            this.onMouseAdded(deviceId);
        }

    }

    public void setReportMotionEvents() {
        this.reportMotionEvents = true;
        Iterator var1 = this.gameControllers.iterator();

        while(var1.hasNext()) {
            GameControllerInfo controller = (GameControllerInfo)var1.next();
            controller.GetListener().setReportMotionEvents();
        }

    }

    public float getBatteryLevel(int deviceId) {
        if (VERSION.SDK_INT >= 31) {
            InputDevice inputDevice = this.inputManager.getInputDevice(deviceId);
            if (inputDevice != null) {
                BatteryState batteryState = inputDevice.getBatteryState();
                if (batteryState != null && batteryState.isPresent()) {
                    float batteryLevel = batteryState.getCapacity();
                    return batteryLevel;
                }
            }
        }

        return 1.0F;
    }

    public int getBatteryStatus(int deviceId) {
        if (VERSION.SDK_INT >= 31) {
            InputDevice inputDevice = this.inputManager.getInputDevice(deviceId);
            if (inputDevice != null) {
                BatteryState batteryState = inputDevice.getBatteryState();
                if (batteryState != null && batteryState.isPresent()) {
                    int batteryStatus = batteryState.getStatus();
                    return batteryStatus;
                }
            }
        }

        return 1;
    }

    public void setLight(int deviceId, int lightType, int lightValue) {
        for(int index = 0; index < this.gameControllers.size(); ++index) {
            GameControllerInfo controller = (GameControllerInfo)this.gameControllers.get(index);
            if (controller.GetGameControllerDeviceId() == deviceId) {
                controller.GetListener().setLight(lightType, lightValue);
                break;
            }
        }

    }

    @SuppressLint({"NewApi"})
    private void updateVibrator(Vibrator vibrator, int intensity, int duration) {
        if (vibrator != null) {
            if (intensity == 0) {
                vibrator.cancel();
            } else if (duration > 0) {
                vibrator.vibrate(VibrationEffect.createOneShot((long)duration, intensity));
            }
        }

    }

    void setVibrationMultiChannel(InputDevice inputDevice, int leftIntensity, int leftDuration, int rightIntensity, int rightDuration) {
        if (VERSION.SDK_INT >= 31) {
            VibratorManager vibratorManager = inputDevice.getVibratorManager();
            if (vibratorManager != null) {
                int[] vibratorIds = vibratorManager.getVibratorIds();
                int vibratorCount = vibratorIds.length;
                Log.d("GameControllerManager", "Vibrator Count: " + vibratorCount);
                if (vibratorCount > 0) {
                    this.updateVibrator(vibratorManager.getVibrator(vibratorIds[0]), leftIntensity, leftDuration);
                    if (vibratorCount > 1) {
                        this.updateVibrator(vibratorManager.getVibrator(vibratorIds[1]), rightIntensity, rightDuration);
                    }
                }
            }
        }

    }

    public void setVibration(int deviceId, int leftIntensity, int leftDuration, int rightIntensity, int rightDuration) {
        InputDevice inputDevice = this.inputManager.getInputDevice(deviceId);
        if (inputDevice != null) {
            if (VERSION.SDK_INT >= 31) {
                this.setVibrationMultiChannel(inputDevice, leftIntensity, leftDuration, rightIntensity, rightDuration);
            } else if (VERSION.SDK_INT >= 26) {
                Vibrator deviceVibrator = inputDevice.getVibrator();
                this.updateVibrator(deviceVibrator, leftIntensity, leftDuration);
            }
        }

    }

    public String getDeviceNameById(int deviceId) {
        InputDevice inputDevice = this.inputManager.getInputDevice(deviceId);
        return inputDevice != null ? inputDevice.getName() : "";
    }

    private void notifyNativeConnection(GameControllerInfo gcInfo) {
        this.onControllerConnected(gcInfo.GetGameControllerDeviceInfoArray(), gcInfo.GetGameControllerAxisMinArray(), gcInfo.GetGameControllerAxisMaxArray(), gcInfo.GetGameControllerAxisFlatArray(), gcInfo.GetGameControllerAxisFuzzArray());
    }

    private String generateSourceString(int source) {
        String sourceString = "Source Classes: ";
        int sourceMasked = source & -256;
        int sourceClass = source & 255;
        if ((sourceClass & 1) != 0) {
            sourceString = sourceString + "BUTTON ";
        }

        if ((sourceClass & 16) != 0) {
            sourceString = sourceString + "JOYSTICK ";
        }

        if ((sourceClass & 2) != 0) {
            sourceString = sourceString + "POINTER ";
        }

        if ((sourceClass & 8) != 0) {
            sourceString = sourceString + "POSITION ";
        }

        if ((sourceClass & 4) != 0) {
            sourceString = sourceString + "TRACKBALL ";
        }

        sourceString = sourceString + "\nSources: ";
        if ((sourceMasked & '쀂') != 0) {
            sourceString = sourceString + "BLUETOOTH_STYLUS ";
        }

        if ((sourceMasked & 513) != 0) {
            sourceString = sourceString + "DPAD ";
        }

        if ((sourceMasked & 33554433) != 0) {
            sourceString = sourceString + "HDMI ";
        }

        if ((sourceMasked & 16777232) != 0) {
            sourceString = sourceString + "JOYSTICK ";
        }

        if ((sourceMasked & 257) != 0) {
            sourceString = sourceString + "KEYBOARD ";
        }

        if ((sourceMasked & 8194) != 0) {
            sourceString = sourceString + "MOUSE ";
        }

        if ((sourceMasked & 131076) != 0) {
            sourceString = sourceString + "MOUSE_RELATIVE ";
        }

        if ((sourceMasked & 4194304) != 0) {
            sourceString = sourceString + "ROTARY_ENCODER ";
        }

        if ((sourceMasked & 16386) != 0) {
            sourceString = sourceString + "STYLUS ";
        }

        if ((sourceMasked & 1048584) != 0) {
            sourceString = sourceString + "TOUCHPAD ";
        }

        if ((sourceMasked & 4098) != 0) {
            sourceString = sourceString + "TOUCHSCREEN ";
        }

        if ((sourceMasked & 2097152) != 0) {
            sourceString = sourceString + "TOUCH_NAVIGATION ";
        }

        if ((sourceMasked & 65540) != 0) {
            sourceString = sourceString + "TRACKBALL ";
        }

        return sourceString;
    }

    private String getAxisString(int axis) {
        switch(axis) {
            case 0:
                return "AXIS_X";
            case 1:
                return "AXIS_Y";
            case 2:
                return "AXIS_PRESSURE";
            case 3:
                return "AXIS_SIZE";
            case 4:
                return "AXIS_TOUCH_MAJOR";
            case 5:
                return "AXIS_TOUCH_MINOR";
            case 6:
                return "AXIS_TOOL_MAJOR";
            case 7:
                return "AXIS_TOOL_MINOR";
            case 8:
                return "AXIS_ORIENTATION";
            case 9:
                return "AXIS_VSCROLL";
            case 10:
                return "AXIS_HSCROLL";
            case 11:
                return "AXIS_Z";
            case 12:
                return "AXIS_RX";
            case 13:
                return "AXIS_RY";
            case 14:
                return "AXIS_RZ";
            case 15:
                return "AXIS_HAT_X";
            case 16:
                return "AXIS_HAT_Y";
            case 17:
                return "AXIS_LTRIGGER";
            case 18:
                return "AXIS_RTRIGGER";
            case 19:
                return "AXIS_THROTTLE";
            case 20:
                return "AXIS_RUDDER";
            case 21:
                return "AXIS_WHEEL";
            case 22:
                return "AXIS_GAS";
            case 23:
                return "AXIS_BRAKE";
            case 24:
                return "AXIS_DISTANCE";
            case 25:
                return "AXIS_TILT";
            case 26:
                return "AXIS_SCROLL";
            case 27:
                return "AXIS_RELATIVE_X";
            case 28:
                return "AXIS_RELATIVE_Y";
            case 29:
            case 30:
            case 31:
            default:
                return "AXIS_NONE";
            case 32:
                return "AXIS_GENERIC_1";
            case 33:
                return "AXIS_GENERIC_2";
            case 34:
                return "AXIS_GENERIC_3";
            case 35:
                return "AXIS_GENERIC_4";
            case 36:
                return "AXIS_GENERIC_5";
            case 37:
                return "AXIS_GENERIC_6";
            case 38:
                return "AXIS_GENERIC_7";
            case 39:
                return "AXIS_GENERIC_8";
            case 40:
                return "AXIS_GENERIC_9";
            case 41:
                return "AXIS_GENERIC_10";
            case 42:
                return "AXIS_GENERIC_11";
            case 43:
                return "AXIS_GENERIC_12";
            case 44:
                return "AXIS_GENERIC_13";
            case 45:
                return "AXIS_GENERIC_14";
            case 46:
                return "AXIS_GENERIC_15";
            case 47:
                return "AXIS_GENERIC_16";
        }
    }

    private void logMotionRange(InputDevice.MotionRange motionRange) {
        String axisString = this.getAxisString(motionRange.getAxis());
        String axisSourceString = this.generateSourceString(motionRange.getSource());
        float axisFlat = motionRange.getFlat();
        float axisFuzz = motionRange.getFuzz();
        float axisMax = motionRange.getMax();
        float axisMin = motionRange.getMin();
        float axisRange = motionRange.getRange();
        float axisResolution = -1.0F;
        if (VERSION.SDK_INT >= 18) {
            axisResolution = motionRange.getResolution();
        }

        Log.d("GameControllerManager", "MotionRange:\n" + axisString + "\n" + axisSourceString + "\n   Axis Min   : " + axisMin + "\n   Axis Max   : " + axisMax + "\n   Axis Range : " + axisRange + "\n   Axis Flat  : " + axisFlat + "\n   Axis Fuzz  : " + axisFuzz + "\n   Axis Res   : " + axisResolution);
    }

    private void logControllerInfo(int deviceId) {
        InputDevice inputDevice = InputDevice.getDevice(deviceId);
        boolean controllerNumber = true;
        if (VERSION.SDK_INT >= 19) {
            int var14 = inputDevice.getControllerNumber();
        }

        String deviceDescriptor = inputDevice.getDescriptor();
        String deviceName = inputDevice.getName();
        int deviceProductId = -1;
        if (VERSION.SDK_INT >= 19) {
            deviceProductId = inputDevice.getProductId();
        }

        int deviceSources = inputDevice.getSources();
        int deviceVendorId = -1;
        if (VERSION.SDK_INT >= 19) {
            deviceVendorId = inputDevice.getVendorId();
        }

        boolean hasVibrator = inputDevice.getVibrator().hasVibrator();
        boolean isVirtual = inputDevice.isVirtual();
        Log.d("GameControllerManager", "logControllerInfo\nfor deviceId: " + deviceId + "\nname: " + deviceName + "\ndescriptor: " + deviceDescriptor + "\nvendorId: " + deviceVendorId + "\nproductId " + deviceProductId + "\nhasVibrator: " + hasVibrator + "\nisVirtual: " + isVirtual + "\n" + this.generateSourceString(deviceSources));
        List<InputDevice.MotionRange> motionRanges = inputDevice.getMotionRanges();
        Log.d("GameControllerManager", "Motion Range count: " + motionRanges.size());
        Iterator var12 = motionRanges.iterator();

        while(var12.hasNext()) {
            InputDevice.MotionRange motionRange = (InputDevice.MotionRange)var12.next();
            this.logMotionRange(motionRange);
        }

    }

    public native void onControllerConnected(int[] var1, float[] var2, float[] var3, float[] var4, float[] var5);

    public native void onControllerDisconnected(int var1);

    public native void onMotionData(int var1, int var2, long var3, float var5, float var6, float var7);

    public native void onMouseConnected(int var1);

    public native void onMouseDisconnected(int var1);
}
