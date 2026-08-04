import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static const dashboardBg = Color(0xFFF0F4F8);
  static const authBg = Color(0xFF0F172A);
  static const authInputFill = Color(0xFF1E1B2E);
  static const primary = Color(0xFF3B82F6);

  static ThemeData dashboardTheme() => ThemeData(
        brightness: Brightness.light,
        scaffoldBackgroundColor: dashboardBg,
        colorScheme: ColorScheme.fromSeed(seedColor: primary),
        textTheme: GoogleFonts.interTextTheme(),
        useMaterial3: true,
        appBarTheme: AppBarTheme(
          backgroundColor: Colors.white,
          foregroundColor: Colors.black87,
          elevation: 0,
          titleTextStyle: GoogleFonts.outfit(
            fontWeight: FontWeight.w900,
            color: Colors.blueAccent,
            fontSize: 20,
          ),
        ),
        cardTheme: CardThemeData(
          color: Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
      );

  static InputDecoration authInput(String hint) => InputDecoration(
        hintText: hint,
        hintStyle: GoogleFonts.inter(color: Colors.white38),
        filled: true,
        fillColor: authInputFill,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      );

  static InputDecoration dashboardInput(String hint) => InputDecoration(
        hintText: hint,
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
      );

  static String extractError(dynamic e) {
    if (e is DioException) {
      final data = e.response?.data;
      if (data is Map && data['error'] != null) return data['error'].toString();
      if (data is String && data.isNotEmpty) return data;
      return e.message ?? 'Request failed';
    }
    return e.toString();
  }

  static void showError(BuildContext context, dynamic e) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(extractError(e)), backgroundColor: Colors.red.shade700),
    );
  }

  static void showSuccess(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.green.shade700),
    );
  }
}
