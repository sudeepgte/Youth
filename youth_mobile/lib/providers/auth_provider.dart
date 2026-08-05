import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../models/user_model.dart';
import '../services/api_client.dart';

class AuthProvider extends ChangeNotifier {
  AppUser? user;
  bool loading = false;
  String? error;
  bool initialized = false;

  bool get isLoggedIn => user != null;

  Future<void> bootstrap() async {
    final token = await ApiClient.instance.getToken();
    if (token == null) {
      initialized = true;
      notifyListeners();
      return;
    }
    try {
      final res = await ApiClient.instance.dio.get('/api/mobile/me');
      user = AppUser.fromJson(Map<String, dynamic>.from(res.data as Map));
    } catch (_) {
      await ApiClient.instance.setToken(null);
      user = null;
    }
    initialized = true;
    notifyListeners();
  }

  Future<bool> login(String username, String password) async {
    loading = true;
    error = null;
    notifyListeners();
    try {
      final res = await ApiClient.instance.dio.post('/api/mobile/auth/login', data: {
        'username': username.trim(),
        'password': password,
      });
      final data = Map<String, dynamic>.from(res.data as Map);
      await ApiClient.instance.setToken(data['token'] as String);
      user = AppUser.fromJson(Map<String, dynamic>.from(data['user'] as Map));
      loading = false;
      notifyListeners();
      return true;
    } on DioException catch (e) {
      error = _extractError(e);
      loading = false;
      notifyListeners();
      return false;
    } catch (e) {
      error = e.toString();
      loading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> register({
    required String username,
    required String email,
    required String password,
    String? gender,
    String? dob,
    String? collegeName,
  }) async {
    loading = true;
    error = null;
    notifyListeners();
    try {
      await ApiClient.instance.dio.post('/api/mobile/auth/register', data: {
        'username': username.trim(),
        'email': email.trim(),
        'password': password,
        if (gender != null) 'gender': gender,
        if (dob != null) 'dob': dob,
        if (collegeName != null && collegeName.trim().isNotEmpty) 'collegeName': collegeName.trim(),
      });
      loading = false;
      notifyListeners();
      return await login(username, password);
    } on DioException catch (e) {
      error = _extractError(e);
      loading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> forgotPassword({
    required String username,
    required String email,
    required String newPassword,
    required String confirmPassword,
  }) async {
    loading = true;
    error = null;
    notifyListeners();
    try {
      await ApiClient.instance.dio.post('/api/mobile/auth/forgot-password', data: {
        'username': username.trim(),
        'email': email.trim(),
        'newPassword': newPassword,
        'confirmPassword': confirmPassword,
      });
      loading = false;
      notifyListeners();
      return true;
    } on DioException catch (e) {
      error = _extractError(e);
      loading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> refreshMe() async {
    try {
      final res = await ApiClient.instance.dio.get('/api/mobile/me');
      user = AppUser.fromJson(Map<String, dynamic>.from(res.data as Map));
      notifyListeners();
    } catch (_) {}
  }

  Future<void> logout() async {
    try {
      await ApiClient.instance.dio.post('/api/mobile/auth/logout');
    } catch (_) {}
    await ApiClient.instance.setToken(null);
    user = null;
    notifyListeners();
  }

  String _extractError(DioException e) {
    final data = e.response?.data;
    if (data is Map && data['error'] != null) return data['error'].toString();
    if (data is String && data.isNotEmpty) return data;
    return e.message ?? 'Request failed';
  }
}
