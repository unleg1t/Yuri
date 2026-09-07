package ddlc.yuri.utils.client;

import org.lwjgl.input.Keyboard;

public final class KeyUtil {

    public static final int MOUSE_KEY_OFFSET = -100;

    private KeyUtil() {
    }

    public static int mouseButtonToKeyCode(int button) {
        return button + MOUSE_KEY_OFFSET;
    }

    public static boolean isMouseKey(int key) {
        return key < 0;
    }

    public static String getKeyName(int key) {
        if (key == Keyboard.KEY_NONE) {
            return "None";
        }
        if (key < 0) {
            return getMouseButtonName(key - MOUSE_KEY_OFFSET);
        }
        String name = Keyboard.getKeyName(key);
        return name == null ? "Unknown" : name;
    }

    private static String getMouseButtonName(int button) {
        switch (button) {
            case 0:
                return "LMB";
            case 1:
                return "RMB";
            case 2:
                return "MMB";
            default:
                return "Mouse " + (button + 1);
        }
    }

    public static int stringToKey(String keyName) {
        if (keyName == null) return Keyboard.KEY_NONE;

        String name = keyName.trim().toUpperCase();

        switch (name) {
            case "LMB":
            case "MOUSE0":
            case "MOUSE 0":
                return mouseButtonToKeyCode(0);
            case "RMB":
            case "MOUSE1":
            case "MOUSE 1":
                return mouseButtonToKeyCode(1);
            case "MMB":
            case "MIDDLE":
            case "MOUSE2":
            case "MOUSE 2":
                return mouseButtonToKeyCode(2);
            case "MOUSE3":
            case "MOUSE 3":
                return mouseButtonToKeyCode(3);
            case "MOUSE4":
            case "MOUSE 4":
                return mouseButtonToKeyCode(4);
            case "MOUSE5":
            case "MOUSE 5":
                return mouseButtonToKeyCode(5);

            case "A": return Keyboard.KEY_A;
            case "B": return Keyboard.KEY_B;
            case "C": return Keyboard.KEY_C;
            case "D": return Keyboard.KEY_D;
            case "E": return Keyboard.KEY_E;
            case "F": return Keyboard.KEY_F;
            case "G": return Keyboard.KEY_G;
            case "H": return Keyboard.KEY_H;
            case "I": return Keyboard.KEY_I;
            case "J": return Keyboard.KEY_J;
            case "K": return Keyboard.KEY_K;
            case "L": return Keyboard.KEY_L;
            case "M": return Keyboard.KEY_M;
            case "N": return Keyboard.KEY_N;
            case "O": return Keyboard.KEY_O;
            case "P": return Keyboard.KEY_P;
            case "Q": return Keyboard.KEY_Q;
            case "R": return Keyboard.KEY_R;
            case "S": return Keyboard.KEY_S;
            case "T": return Keyboard.KEY_T;
            case "U": return Keyboard.KEY_U;
            case "V": return Keyboard.KEY_V;
            case "W": return Keyboard.KEY_W;
            case "X": return Keyboard.KEY_X;
            case "Y": return Keyboard.KEY_Y;
            case "Z": return Keyboard.KEY_Z;

            case "0": return Keyboard.KEY_0;
            case "1": return Keyboard.KEY_1;
            case "2": return Keyboard.KEY_2;
            case "3": return Keyboard.KEY_3;
            case "4": return Keyboard.KEY_4;
            case "5": return Keyboard.KEY_5;
            case "6": return Keyboard.KEY_6;
            case "7": return Keyboard.KEY_7;
            case "8": return Keyboard.KEY_8;
            case "9": return Keyboard.KEY_9;

            case "F1": return Keyboard.KEY_F1;
            case "F2": return Keyboard.KEY_F2;
            case "F3": return Keyboard.KEY_F3;
            case "F4": return Keyboard.KEY_F4;
            case "F5": return Keyboard.KEY_F5;
            case "F6": return Keyboard.KEY_F6;
            case "F7": return Keyboard.KEY_F7;
            case "F8": return Keyboard.KEY_F8;
            case "F9": return Keyboard.KEY_F9;
            case "F10": return Keyboard.KEY_F10;
            case "F11": return Keyboard.KEY_F11;
            case "F12": return Keyboard.KEY_F12;

            case "LSHIFT": return Keyboard.KEY_LSHIFT;
            case "RSHIFT": return Keyboard.KEY_RSHIFT;
            case "LCONTROL": return Keyboard.KEY_LCONTROL;
            case "RCONTROL": return Keyboard.KEY_RCONTROL;
            case "LALT": return Keyboard.KEY_LMENU;
            case "RALT": return Keyboard.KEY_RMENU;

            case "ESCAPE": return Keyboard.KEY_ESCAPE;
            case "ENTER": return Keyboard.KEY_RETURN;
            case "TAB": return Keyboard.KEY_TAB;
            case "BACKSPACE": return Keyboard.KEY_BACK;
            case "SPACE": return Keyboard.KEY_SPACE;

            case "INSERT": return Keyboard.KEY_INSERT;
            case "DELETE": return Keyboard.KEY_DELETE;
            case "RIGHT": return Keyboard.KEY_RIGHT;
            case "LEFT": return Keyboard.KEY_LEFT;
            case "DOWN": return Keyboard.KEY_DOWN;
            case "UP": return Keyboard.KEY_UP;

            case "HOME": return Keyboard.KEY_HOME;
            case "END": return Keyboard.KEY_END;
            case "PAGE_UP": return Keyboard.KEY_PRIOR;
            case "PAGE_DOWN": return Keyboard.KEY_NEXT;

            default:
                break;
        }

        int key = Keyboard.getKeyIndex(keyName.trim());
        return key <= 0 ? Keyboard.KEY_NONE : key;
    }
}