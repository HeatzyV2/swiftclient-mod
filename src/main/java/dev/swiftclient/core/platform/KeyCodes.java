package dev.swiftclient.core.platform;

/** Stable saved/UI key codes (GLFW), translated at the SDL3 platform boundary. */
public final class KeyCodes {
   private KeyCodes() {}
   private static final int SDL_ONLY = 10000;
   private static final int[][] KEYS = {
      {32, 44}, // KEY_SPACE
      {39, 52}, // KEY_APOSTROPHE
      {44, 54}, // KEY_COMMA
      {45, 45}, // KEY_MINUS
      {46, 55}, // KEY_PERIOD
      {47, 56}, // KEY_SLASH
      {48, 39}, // KEY_0
      {49, 30}, // KEY_1
      {50, 31}, // KEY_2
      {51, 32}, // KEY_3
      {52, 33}, // KEY_4
      {53, 34}, // KEY_5
      {54, 35}, // KEY_6
      {55, 36}, // KEY_7
      {56, 37}, // KEY_8
      {57, 38}, // KEY_9
      {59, 51}, // KEY_SEMICOLON
      {61, 46}, // KEY_EQUALS
      {65, 4}, // KEY_A
      {66, 5}, // KEY_B
      {67, 6}, // KEY_C
      {68, 7}, // KEY_D
      {69, 8}, // KEY_E
      {70, 9}, // KEY_F
      {71, 10}, // KEY_G
      {72, 11}, // KEY_H
      {73, 12}, // KEY_I
      {74, 13}, // KEY_J
      {75, 14}, // KEY_K
      {76, 15}, // KEY_L
      {77, 16}, // KEY_M
      {78, 17}, // KEY_N
      {79, 18}, // KEY_O
      {80, 19}, // KEY_P
      {81, 20}, // KEY_Q
      {82, 21}, // KEY_R
      {83, 22}, // KEY_S
      {84, 23}, // KEY_T
      {85, 24}, // KEY_U
      {86, 25}, // KEY_V
      {87, 26}, // KEY_W
      {88, 27}, // KEY_X
      {89, 28}, // KEY_Y
      {90, 29}, // KEY_Z
      {91, 47}, // KEY_LBRACKET
      {92, 49}, // KEY_BACKSLASH
      {93, 48}, // KEY_RBRACKET
      {96, 53}, // KEY_GRAVE
      {256, 41}, // KEY_ESCAPE
      {257, 40}, // KEY_RETURN
      {258, 43}, // KEY_TAB
      {259, 42}, // KEY_BACKSPACE
      {260, 73}, // KEY_INSERT
      {261, 76}, // KEY_DELETE
      {262, 79}, // KEY_RIGHT
      {263, 80}, // KEY_LEFT
      {264, 81}, // KEY_DOWN
      {265, 82}, // KEY_UP
      {266, 75}, // KEY_PAGEUP
      {267, 78}, // KEY_PAGEDOWN
      {268, 74}, // KEY_HOME
      {269, 77}, // KEY_END
      {280, 57}, // KEY_CAPSLOCK
      {281, 71}, // KEY_SCROLLLOCK
      {282, 83}, // KEY_NUMLOCK
      {283, 70}, // KEY_PRINTSCREEN
      {284, 72}, // KEY_PAUSE
      {290, 58}, // KEY_F1
      {291, 59}, // KEY_F2
      {292, 60}, // KEY_F3
      {293, 61}, // KEY_F4
      {294, 62}, // KEY_F5
      {295, 63}, // KEY_F6
      {296, 64}, // KEY_F7
      {297, 65}, // KEY_F8
      {298, 66}, // KEY_F9
      {299, 67}, // KEY_F10
      {300, 68}, // KEY_F11
      {301, 69}, // KEY_F12
      {302, 104}, // KEY_F13
      {303, 105}, // KEY_F14
      {304, 106}, // KEY_F15
      {305, 107}, // KEY_F16
      {306, 108}, // KEY_F17
      {307, 109}, // KEY_F18
      {308, 110}, // KEY_F19
      {309, 111}, // KEY_F20
      {310, 112}, // KEY_F21
      {311, 113}, // KEY_F22
      {312, 114}, // KEY_F23
      {313, 115}, // KEY_F24
      {320, 98}, // KEY_NUMPAD0
      {321, 89}, // KEY_NUMPAD1
      {322, 90}, // KEY_NUMPAD2
      {323, 91}, // KEY_NUMPAD3
      {324, 92}, // KEY_NUMPAD4
      {325, 93}, // KEY_NUMPAD5
      {326, 94}, // KEY_NUMPAD6
      {327, 95}, // KEY_NUMPAD7
      {328, 96}, // KEY_NUMPAD8
      {329, 97}, // KEY_NUMPAD9
      {330, 220}, // KEY_NUMPADCOMMA
      {332, 85}, // KEY_MULTIPLY
      {334, 87}, // KEY_ADD
      {335, 88}, // KEY_NUMPADENTER
      {336, 103}, // KEY_NUMPADEQUALS
      {340, 225}, // KEY_LSHIFT
      {341, 224}, // KEY_LCONTROL
      {342, 226}, // KEY_LALT
      {343, 227}, // KEY_LSUPER
      {344, 229}, // KEY_RSHIFT
      {345, 228}, // KEY_RCONTROL
      {346, 230}, // KEY_RALT
      {347, 231}, // KEY_RSUPER
      {331, 84}, // keypad divide
      {333, 86}, // keypad subtract
      {348, 101}, // application/menu
   };

   public static int fromSdl(int key) {
      if (key <= 0) return -1;
      for (int[] pair : KEYS) if (pair[1] == key) return pair[0];
      return SDL_ONLY + key;
   }

   public static int toSdl(int key) {
      if (key >= SDL_ONLY) return key - SDL_ONLY;
      for (int[] pair : KEYS) if (pair[0] == key) return pair[1];
      return 0;
   }

   public static int mouseFromSdl(int button) {
      return switch (button) { case 1 -> 0; case 3 -> 1; case 2 -> 2; default -> button - 1; };
   }
}