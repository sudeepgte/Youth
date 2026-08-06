import 'package:flutter/foundation.dart';

/// Lets drawers on nested pages switch the MainShell tab after popping to root.
class ShellNav {
  ShellNav._();

  static final ValueNotifier<int?> tabRequest = ValueNotifier<int?>(null);

  static void requestTab(int index) {
    tabRequest.value = index;
  }

  static void clear() {
    tabRequest.value = null;
  }
}
