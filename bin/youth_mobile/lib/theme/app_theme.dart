import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Matches web `static/css/style.css` Cool Blue / Ice / Cyan dashboard theme.
class AppTheme {
  // Web CSS variables
  static const bgColor = Color(0xFFE0F2FE); // --bg-color
  static const primary = Color(0xFF3B82F6); // --primary
  static const secondary = Color(0xFF93C5FD); // --secondary
  static const accent = Color(0xFF22D3EE); // --accent
  static const accentHover = Color(0xFF0EA5E9); // --accent-hover
  static const textPrimary = Color(0xFF0F172A); // --text-primary
  static const textSecondary = Color(0xFF334155); // --text-secondary
  static const textMuted = Color(0xFF64748B); // --text-muted
  static const cardBg = Color(0xD9FFFFFF); // --card-bg ≈ rgba(255,255,255,0.85)
  static const glassBorder = Color(0x4DFFFFFF); // --glass-border
  static const gold = Color(0xFFF5C542);

  /// Prefer these names across the app (aliases).
  static const dashboardBg = bgColor;
  static const fieldText = textPrimary;
  static const fieldHint = textMuted;

  static const authBg = Color(0xFF0F172A);
  static const authInputFill = Color(0xFF1E1B2E);

  static const bgGradient = LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [Color(0xFFE0F2FE), Color(0xFFFFFFFF)],
  );

  static const brandGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF3B82F6), Color(0xFF93C5FD), Color(0xFF22D3EE)],
  );

  static BoxDecoration cardDecoration({double radius = 16}) => BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: const Color(0x33FFFFFF)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x1A0F172A),
            blurRadius: 24,
            offset: Offset(0, 12),
          ),
        ],
      );

  static ThemeData dashboardTheme() => ThemeData(
        brightness: Brightness.light,
        scaffoldBackgroundColor: bgColor,
        colorScheme: const ColorScheme.light(
          primary: primary,
          secondary: secondary,
          tertiary: accent,
          surface: Colors.white,
          onSurface: textPrimary,
          onPrimary: Colors.white,
        ),
        textTheme: GoogleFonts.outfitTextTheme(ThemeData.light().textTheme).apply(
          bodyColor: textPrimary,
          displayColor: textPrimary,
        ),
        useMaterial3: true,
        appBarTheme: AppBarTheme(
          backgroundColor: const Color(0xE6FFFFFF),
          foregroundColor: textPrimary,
          elevation: 0,
          surfaceTintColor: Colors.transparent,
          titleTextStyle: GoogleFonts.outfit(
            fontWeight: FontWeight.w800,
            color: primary,
            fontSize: 20,
          ),
          iconTheme: const IconThemeData(color: textPrimary),
        ),
        cardTheme: CardThemeData(
          color: cardBg,
          elevation: 0,
          surfaceTintColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: const BorderSide(color: Color(0x33B6D4FE)),
          ),
        ),
        dividerColor: const Color(0xFFBFDBFE),
        chipTheme: ChipThemeData(
          backgroundColor: Colors.white.withValues(alpha: 0.82),
          selectedColor: primary.withValues(alpha: 0.15),
          labelStyle: GoogleFonts.outfit(color: textSecondary, fontWeight: FontWeight.w600),
          side: const BorderSide(color: Color(0xFFBFDBFE)),
        ),
        listTileTheme: ListTileThemeData(
          tileColor: Colors.white,
          selectedTileColor: const Color(0xFFEFF6FF),
          iconColor: textSecondary,
          textColor: textPrimary,
          titleTextStyle: GoogleFonts.outfit(fontWeight: FontWeight.w600, color: textPrimary),
          subtitleTextStyle: GoogleFonts.outfit(color: textMuted, fontSize: 13),
        ),
        bottomAppBarTheme: const BottomAppBarThemeData(
          color: Color(0xF2FFFFFF),
          elevation: 8,
          surfaceTintColor: Colors.transparent,
        ),
        floatingActionButtonTheme: const FloatingActionButtonThemeData(
          backgroundColor: primary,
          foregroundColor: Colors.white,
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white.withValues(alpha: 0.92),
          hintStyle: GoogleFonts.outfit(color: textMuted, fontSize: 14),
          labelStyle: GoogleFonts.outfit(color: textMuted, fontSize: 14),
          floatingLabelStyle: GoogleFonts.outfit(color: primary, fontSize: 14),
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFFBFDBFE)),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: Color(0xFFBFDBFE)),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: primary, width: 1.5),
          ),
        ),
        dropdownMenuTheme: DropdownMenuThemeData(
          textStyle: GoogleFonts.outfit(color: textPrimary, fontSize: 14),
        ),
        textSelectionTheme: const TextSelectionThemeData(
          cursorColor: primary,
          selectionColor: Color(0x333B82F6),
        ),
      );

  static ThemeData authTheme() => ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: authBg,
        colorScheme: const ColorScheme.dark(
          primary: primary,
          secondary: accent,
          surface: authInputFill,
          onSurface: Colors.white,
        ),
        textTheme: GoogleFonts.outfitTextTheme(ThemeData.dark().textTheme),
        useMaterial3: true,
        listTileTheme: ListTileThemeData(
          tileColor: authInputFill,
          selectedTileColor: primary.withValues(alpha: 0.15),
          iconColor: Colors.white70,
          textColor: Colors.white,
        ),
      );

  static InputDecoration authInput(String hint) => InputDecoration(
        hintText: hint,
        hintStyle: GoogleFonts.outfit(color: Colors.white60),
        filled: true,
        fillColor: authInputFill,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      );

  static InputDecoration dashboardInput(String hint) => InputDecoration(
        hintText: hint,
        hintStyle: GoogleFonts.outfit(color: textMuted, fontSize: 14),
        labelStyle: GoogleFonts.outfit(color: textMuted, fontSize: 14),
        floatingLabelStyle: GoogleFonts.outfit(color: primary, fontSize: 14),
        filled: true,
        fillColor: Colors.white.withValues(alpha: 0.92),
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFBFDBFE)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFBFDBFE)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: primary, width: 1.5),
        ),
      );

  static String extractError(dynamic e) {
    if (e is DioException) {
      final data = e.response?.data;
      if (data is Map && data['error'] != null) return data['error'].toString();
      if (data is Map && data['message'] != null) return data['message'].toString();
      if (data is String && data.isNotEmpty && data.length < 200) return data;
      final code = e.response?.statusCode;
      if (code == 400) return 'Could not send — check the message and try again';
      if (code == 401) return 'Session expired — please log in again';
      if (code == 403) return 'You do not have permission for this action';
      if (code == 413) return 'File is too large';
      if (code != null && code >= 500) return 'Server error — please try again';
      final msg = e.message ?? '';
      if (msg.contains('status code of')) {
        return code != null ? 'Request failed ($code)' : 'Request failed';
      }
      return msg.isNotEmpty ? msg : 'Request failed';
    }
    return e.toString();
  }

  static void showError(BuildContext context, dynamic e) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(extractError(e)),
        backgroundColor: Colors.red.shade700,
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  static void showSuccess(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.green.shade700),
    );
  }

  /// Bottom sheets with ListTiles need an opaque Material background for ink splashes.
  static Future<T?> showBottomSheet<T>(
    BuildContext context,
    Widget Function(BuildContext sheetContext) builder, {
    bool isScrollControlled = false,
  }) {
    return showModalBottomSheet<T>(
      context: context,
      isScrollControlled: isScrollControlled,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (sheetContext) => Material(
        color: Colors.white,
        child: builder(sheetContext),
      ),
    );
  }
}
