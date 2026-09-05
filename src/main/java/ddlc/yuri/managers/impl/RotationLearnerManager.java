package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RotationLearnerManager implements IMinecraft {

    public static final RotationLearnerManager INSTANCE = new RotationLearnerManager();

    private static final File PRESET_DIR = new File(mc.mcDataDir, "Yuri/rotationpresets");
    private static final String BUNDLED_PATH = "/assets/minecraft/yuri/rotationpresets/";
    private static final String BUNDLED_MANIFEST = BUNDLED_PATH + "manifest.txt";

    private static final double CAPTURE_RANGE = 6.0;
    private static final int RESERVOIR_CAPACITY = 300;
    private static final int FLUSH_INTERVAL = 20;
    private static final Random RANDOM = new Random();

    private static final ThreadPoolExecutor IO_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(512), r -> {
        Thread t = new Thread(r, "yuri-rotation-io");
        t.setDaemon(true);
        return t;
    }, new ThreadPoolExecutor.DiscardOldestPolicy());

    private static final Map<Integer, Float> lastYaw = new HashMap<>();
    private static final Map<Integer, Float> lastPitch = new HashMap<>();
    private static final Map<Integer, Boolean> lastSwing = new HashMap<>();

    private static volatile boolean recording = false;
    private static volatile String activePresetName;
    private static BufferedWriter activeWriter;
    private static int flushCounter;
    private static volatile RotationModel activeModel;
    private static volatile String activeModelName;

    private static volatile float lastAppliedYawDelta = 0f;
    private static volatile float lastAppliedPitchDelta = 0f;

    private RotationLearnerManager() {
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!recording) return;

        for (EntityPlayer other : mc.theWorld.playerEntities) {
            if (other == mc.thePlayer) continue;

            int id = other.getEntityId();
            float yaw = other.rotationYaw;
            float pitch = other.rotationPitch;
            boolean swinging = other.isSwingInProgress;

            Float prevYaw = lastYaw.get(id);
            Float prevPitch = lastPitch.get(id);
            Boolean prevSwing = lastSwing.get(id);

            if (recording && prevSwing != null && !prevSwing && swinging && prevYaw != null
                    && mc.thePlayer.getDistanceToEntity(other) <= CAPTURE_RANGE) {
                float yawDelta = MathHelper.wrapAngleTo180_float(yaw - prevYaw);
                float pitchDelta = pitch - prevPitch;
                if (yawDelta != 0f && pitchDelta != 0f) {
                    queueSample(yawDelta, pitchDelta);
                }
            }

            lastYaw.put(id, yaw);
            lastPitch.put(id, pitch);
            lastSwing.put(id, swinging);
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        lastYaw.clear();
        lastPitch.clear();
        lastSwing.clear();
    }

    private static void queueSample(float yawDelta, float pitchDelta) {
        IO_EXECUTOR.execute(() -> writeSampleInternal(yawDelta, pitchDelta));
    }

    private static void writeSampleInternal(float yawDelta, float pitchDelta) {
        if (activeWriter == null) return;
        try {
            activeWriter.write(yawDelta + "," + pitchDelta);
            activeWriter.newLine();
            flushCounter++;
            if (flushCounter >= FLUSH_INTERVAL) {
                activeWriter.flush();
                flushCounter = 0;
            }
        } catch (IOException ignored) {
        }
    }

    public static void startRecording(String name) {
        stopRecording();
        recording = true;
        activePresetName = name;
        IO_EXECUTOR.execute(() -> {
            try {
                PRESET_DIR.mkdirs();
                activeWriter = new BufferedWriter(new FileWriter(getPresetFile(name), true));
                flushCounter = 0;
            } catch (IOException ignored) {
            }
        });
    }

    public static void stopRecording() {
        if (!recording) return;
        recording = false;
        IO_EXECUTOR.execute(RotationLearnerManager::closeWriterQuietly);
    }

    private static void closeWriterQuietly() {
        if (activeWriter == null) return;
        try {
            activeWriter.flush();
            activeWriter.close();
        } catch (IOException ignored) {
        }
        activeWriter = null;
    }

    public static boolean loadPreset(String name) {
        RotationModel model;

        File file = getPresetFile(name);
        if (file.exists()) {
            model = parsePresetStream(fileStreamOrNull(file));
        } else {
            model = parsePresetStream(bundledStreamOrNull(name));
        }

        if (model == null) return false;

        activeModel = model;
        activeModelName = name;
        return true;
    }

    private static InputStream fileStreamOrNull(File file) {
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            return null;
        }
    }

    private static InputStream bundledStreamOrNull(String name) {
        return RotationLearnerManager.class.getResourceAsStream(BUNDLED_PATH + name + ".csv");
    }

    private static RotationModel parsePresetStream(InputStream input) {
        if (input == null) return null;

        double yawSum = 0, pitchSum = 0;
        double yawSumSq = 0, pitchSumSq = 0;
        int count = 0;
        List<Float> yawReservoir = new ArrayList<>(RESERVOIR_CAPACITY);
        List<Float> pitchReservoir = new ArrayList<>(RESERVOIR_CAPACITY);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int comma = line.indexOf(',');
                if (comma <= 0) continue;

                float yawDelta;
                float pitchDelta;
                try {
                    yawDelta = Float.parseFloat(line.substring(0, comma));
                    pitchDelta = Float.parseFloat(line.substring(comma + 1));
                } catch (NumberFormatException e) {
                    continue;
                }

                yawSum += yawDelta;
                pitchSum += pitchDelta;
                yawSumSq += yawDelta * (double) yawDelta;
                pitchSumSq += pitchDelta * (double) pitchDelta;
                count++;

                if (yawReservoir.size() < RESERVOIR_CAPACITY) {
                    yawReservoir.add(yawDelta);
                    pitchReservoir.add(pitchDelta);
                } else {
                    int replaceIndex = RANDOM.nextInt(count);
                    if (replaceIndex < RESERVOIR_CAPACITY) {
                        yawReservoir.set(replaceIndex, yawDelta);
                        pitchReservoir.set(replaceIndex, pitchDelta);
                    }
                }
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }

        if (count == 0) return null;

        float yawMean = (float) (yawSum / count);
        float pitchMean = (float) (pitchSum / count);
        float yawVariance = (float) Math.max(0, yawSumSq / count - yawMean * yawMean);
        float pitchVariance = (float) Math.max(0, pitchSumSq / count - pitchMean * pitchMean);

        RotationModel model = new RotationModel();
        model.yawMean = yawMean;
        model.pitchMean = pitchMean;
        model.yawStdDev = (float) Math.sqrt(yawVariance);
        model.pitchStdDev = (float) Math.sqrt(pitchVariance);
        model.sampleCount = count;
        model.yawReservoir = yawReservoir;
        model.pitchReservoir = pitchReservoir;
        return model;
    }

    public static void unloadPreset() {
        activeModel = null;
        activeModelName = null;
    }

    public static List<String> listPresets() {
        LinkedHashSet<String> names = new LinkedHashSet<>();

        PRESET_DIR.mkdirs();
        File[] files = PRESET_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".csv")) {
                    names.add(name.substring(0, name.length() - 4));
                }
            }
        }

        names.addAll(listBundledPresets());

        return new ArrayList<>(names);
    }

    private static List<String> listBundledPresets() {
        List<String> names = new ArrayList<>();
        try (InputStream input = RotationLearnerManager.class.getResourceAsStream(BUNDLED_MANIFEST)) {
            if (input == null) return names;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        names.add(line);
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return names;
    }

    public static boolean isBundledPreset(String name) {
        return !getPresetFile(name).exists() && listBundledPresets().contains(name);
    }

    public static boolean deletePreset(String name) {
        File file = getPresetFile(name);
        return file.exists() && file.delete();
    }

    public static boolean isRecording() {
        return recording;
    }

    public static String getActivePresetName() {
        return activePresetName;
    }

    public static boolean hasModelLoaded() {
        return activeModel != null;
    }

    public static String getLoadedModelName() {
        return activeModelName;
    }

    public static void resetSmoothing() {
        lastAppliedYawDelta = 0f;
        lastAppliedPitchDelta = 0f;
    }

    public static Vector2f humanize(Vector2f rotation) {
        return humanize(rotation, 1.0f, 1.0f);
    }

    public static Vector2f humanize(Vector2f rotation, float weight, float ease) {
        RotationModel model = activeModel;
        if (model == null) return rotation;

        float rawYawDelta;
        float rawPitchDelta;

        if (!model.yawReservoir.isEmpty() && RANDOM.nextBoolean()) {
            int index = RANDOM.nextInt(model.yawReservoir.size());
            rawYawDelta = model.yawReservoir.get(index);
            rawPitchDelta = model.pitchReservoir.get(index);
        } else {
            rawYawDelta = (float) (model.yawMean + RANDOM.nextGaussian() * model.yawStdDev);
            rawPitchDelta = (float) (model.pitchMean + RANDOM.nextGaussian() * model.pitchStdDev);
        }

        float clampedEase = MathHelper.clamp_float(ease, 0.01f, 1.0f);
        float easedYawDelta = lastAppliedYawDelta + (rawYawDelta - lastAppliedYawDelta) * clampedEase;
        float easedPitchDelta = lastAppliedPitchDelta + (rawPitchDelta - lastAppliedPitchDelta) * clampedEase;

        lastAppliedYawDelta = easedYawDelta;
        lastAppliedPitchDelta = easedPitchDelta;

        float yaw = rotation.x + easedYawDelta * weight;
        float pitch = MathHelper.clamp_float(rotation.y + easedPitchDelta * weight, -90f, 90f);

        return new Vector2f(yaw, pitch);
    }

    private static File getPresetFile(String name) {
        return new File(PRESET_DIR, name + ".csv");
    }

    public static String exportPresetRaw(String name) {
        File file = getPresetFile(name);
        if (!file.exists()) return null;
        try {
            byte[] bytes = readAllBytes(file);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean importPresetRaw(String name, String base64Data) {
        if (name == null || base64Data == null) return false;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            PRESET_DIR.mkdirs();
            try (FileOutputStream out = new FileOutputStream(getPresetFile(name))) {
                out.write(bytes);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static class RotationModel {
        float yawMean;
        float pitchMean;
        float yawStdDev;
        float pitchStdDev;
        int sampleCount;
        List<Float> yawReservoir;
        List<Float> pitchReservoir;
    }
}