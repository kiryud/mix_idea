import 'dart:async';
import 'dart:convert';
import 'dart:io' show Platform;
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/models.dart';

class ApiException implements Exception {
  final int statusCode;
  final int? code;
  final String message;

  ApiException({required this.statusCode, this.code, required this.message});

  @override
  String toString() => message;
}

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal();

  static const String _prefBaseUrlKey = 'api_base_url';
  static const String _prefSessionKey = 'api_user_session';

  String _baseUrl = '';
  UserSession? _session;
  bool _initialized = false;
  Completer<void>? _refreshCompleter;

  UserSession? get session => _session;
  bool get isLoggedIn => _session != null && _session!.accessToken.isNotEmpty;
  String get baseUrl => _baseUrl;

  static String getDefaultBaseUrl() {
    if (kIsWeb) {
      return 'http://localhost:8080';
    }
    try {
      if (Platform.isAndroid) {
        // Android 에뮬레이터 기본 게이트웨이
        return 'http://10.0.2.2:8080';
      }
    } catch (_) {}
    return 'http://localhost:8080';
  }

  Future<void> init() async {
    if (_initialized) return;
    final prefs = await SharedPreferences.getInstance();
    _baseUrl = prefs.getString(_prefBaseUrlKey) ?? getDefaultBaseUrl();

    final sessionStr = prefs.getString(_prefSessionKey);
    if (sessionStr != null) {
      try {
        _session = UserSession.fromJson(jsonDecode(sessionStr));
      } catch (_) {
        await prefs.remove(_prefSessionKey);
      }
    }
    _initialized = true;
  }

  Future<void> setBaseUrl(String url) async {
    _baseUrl = url.trim().replaceAll(RegExp(r'/+$'), '');
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefBaseUrlKey, _baseUrl);
  }

  Future<void> _saveSession(UserSession? session) async {
    _session = session;
    final prefs = await SharedPreferences.getInstance();
    if (session != null) {
      await prefs.setString(_prefSessionKey, jsonEncode(session.toJson()));
    } else {
      await prefs.remove(_prefSessionKey);
    }
  }

  Future<void> logout() async {
    await _saveSession(null);
  }

  Uri _uri(String path, [Map<String, dynamic>? queryParams]) {
    final cleanPath = path.startsWith('/') ? path : '/$path';
    final urlStr = '$_baseUrl$cleanPath';
    final uri = Uri.parse(urlStr);
    if (queryParams != null && queryParams.isNotEmpty) {
      return uri.replace(
        queryParameters: queryParams.map(
          (k, v) => MapEntry(k, v?.toString() ?? ''),
        ),
      );
    }
    return uri;
  }

  Future<dynamic> _request(
    String path, {
    String method = 'GET',
    Map<String, dynamic>? body,
    Map<String, dynamic>? queryParams,
    bool auth = true,
    bool retry = true,
  }) async {
    await init();
    final url = _uri(path, queryParams);
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    if (auth && _session != null && _session!.accessToken.isNotEmpty) {
      headers['Authorization'] = 'Bearer ${_session!.accessToken}';
    }

    http.Response response;
    try {
      final reqBody = body != null ? jsonEncode(body) : null;
      switch (method.toUpperCase()) {
        case 'POST':
          response = await http.post(url, headers: headers, body: reqBody);
          break;
        case 'PUT':
          response = await http.put(url, headers: headers, body: reqBody);
          break;
        case 'DELETE':
          response = await http.delete(url, headers: headers, body: reqBody);
          break;
        case 'GET':
        default:
          response = await http.get(url, headers: headers);
          break;
      }
    } catch (e) {
      throw ApiException(
        statusCode: 0,
        message: '서버에 연결할 수 없습니다 ($_baseUrl).\n서버 실행 상태 및 네트워크를 확인해 주세요.',
      );
    }

    // 401 토큰 만료 처리
    if (response.statusCode == 401 && auth && retry && _session?.refreshToken != null) {
      final renewed = await _tryRefreshToken();
      if (renewed) {
        return _request(path,
            method: method,
            body: body,
            queryParams: queryParams,
            auth: auth,
            retry: false);
      }
    }

    dynamic responseData;
    try {
      if (response.body.isNotEmpty) {
        responseData = jsonDecode(utf8.decode(response.bodyBytes));
      }
    } catch (_) {
      responseData = null;
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return responseData;
    }

    String errorMsg = '요청을 처리하지 못했습니다 (HTTP ${response.statusCode})';
    int? errorCode;
    if (responseData is Map<String, dynamic>) {
      if (responseData['message'] != null) {
        errorMsg = responseData['message'].toString();
      }
      if (responseData['code'] is num) {
        errorCode = (responseData['code'] as num).toInt();
      }
    }

    throw ApiException(
      statusCode: response.statusCode,
      code: errorCode,
      message: errorMsg,
    );
  }

  Future<bool> _tryRefreshToken() async {
    if (_refreshCompleter != null) {
      await _refreshCompleter!.future;
      return isLoggedIn;
    }
    _refreshCompleter = Completer<void>();
    try {
      final refreshUrl = _uri('/app/user/refresh-token');
      final res = await http.post(
        refreshUrl,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': _session!.refreshToken}),
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(utf8.decode(res.bodyBytes));
        final newAccess = data['accessToken'] as String?;
        if (newAccess != null && newAccess.isNotEmpty) {
          _session = UserSession(
            uid: _session!.uid,
            id: _session!.id,
            name: _session!.name,
            email: _session!.email,
            accessToken: newAccess,
            refreshToken: _session!.refreshToken,
          );
          await _saveSession(_session);
          _refreshCompleter!.complete();
          _refreshCompleter = null;
          return true;
        }
      }
    } catch (_) {}
    _refreshCompleter!.complete();
    _refreshCompleter = null;
    await logout();
    return false;
  }

  // 1. 회원가입
  Future<void> signUp({
    required String id,
    required String pw,
    required String email,
    required String name,
    required String birth,
  }) async {
    await _request(
      '/app/user/sign-up',
      method: 'POST',
      auth: false,
      body: {
        'id': id,
        'pw': pw,
        'email': email,
        'name': name,
        'birth': birth,
      },
    );
  }

  // 2. 로그인
  Future<UserSession> login({
    required String id,
    required String pw,
  }) async {
    final res = await _request(
      '/app/user/login',
      method: 'POST',
      auth: false,
      body: {'id': id, 'pw': pw},
    );

    final accessToken = res['accessToken'] as String? ?? '';
    final refreshToken = res['refreshToken'] as String? ?? '';

    // 로그인 후 프로필 조회 시도
    String name = id;
    String email = '';
    String? uid;

    _session = UserSession(
      id: id,
      name: name,
      email: email,
      accessToken: accessToken,
      refreshToken: refreshToken,
    );

    try {
      final profile = await _request('/app/user/profile', auth: true);
      if (profile is Map<String, dynamic>) {
        name = profile['name'] as String? ?? id;
        email = profile['email'] as String? ?? '';
        uid = profile['uid'] as String?;
      }
    } catch (_) {}

    _session = UserSession(
      uid: uid,
      id: id,
      name: name,
      email: email,
      accessToken: accessToken,
      refreshToken: refreshToken,
    );
    await _saveSession(_session);
    return _session!;
  }

  // 3. 카테고리 목록 조회
  Future<List<CategoryItem>> getCategories() async {
    final res = await _request('/app/categories', auth: true);
    final items = res['items'] as List<dynamic>? ?? [];
    return items.map((e) => CategoryItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  // 4. 새 카테고리 추가
  Future<CategoryItem> createCategory({
    required String name,
    String guideType = 'general',
  }) async {
    final res = await _request(
      '/app/categories',
      method: 'POST',
      auth: true,
      body: {'name': name, 'guideType': guideType},
    );
    final catData = res['category'] as Map<String, dynamic>? ?? res;
    return CategoryItem.fromJson(catData);
  }

  // 5. 단어 뽑기 (초기 뽑기 / 리롤 / 단어 고정 / 방향성)
  Future<List<WordItem>> drawWords({
    required String categoryId,
    List<String> lockedWordIds = const [],
    List<String> previousWordIds = const [],
    String direction = '',
  }) async {
    final res = await _request(
      '/app/words/draw',
      method: 'POST',
      auth: true,
      body: {
        'categoryId': categoryId,
        'lockedWordIds': lockedWordIds,
        'previousWordIds': previousWordIds,
        'direction': direction,
      },
    );
    final rawWords = res['words'] as List<dynamic>? ?? [];
    return rawWords
        .map((w) => WordItem.fromJson(w as Map<String, dynamic>))
        .toList();
  }

  // 6. 아이디어 생성
  Future<IdeaItem> createIdea({
    required String categoryId,
    required String categoryName,
    required List<String> wordIds,
    String direction = '',
  }) async {
    final res = await _request(
      '/app/ideas',
      method: 'POST',
      auth: true,
      body: {
        'categoryId': categoryId,
        'wordIds': wordIds,
        'direction': direction,
      },
    );
    final ideaData = res['idea'] as Map<String, dynamic>? ?? res;
    return IdeaItem.fromJson(ideaData, categoryName: categoryName);
  }

  // 7. 아이디어 재조합 (다시 만들기)
  Future<IdeaItem> recombineIdea({
    required String ideaId,
    required String categoryName,
  }) async {
    final res = await _request(
      '/app/ideas/$ideaId/recombine',
      method: 'POST',
      auth: true,
    );
    final ideaData = res['idea'] as Map<String, dynamic>? ?? res;
    return IdeaItem.fromJson(ideaData, categoryName: categoryName);
  }

  // 8. 분야별 후속 가이드 조회/생성
  Future<GuideItem> getGuide(String ideaId) async {
    final res = await _request(
      '/app/ideas/$ideaId/guide',
      method: 'POST',
      auth: true,
    );
    final guideData = res['guide'] as Map<String, dynamic>? ?? res;
    return GuideItem.fromJson(guideData);
  }

  // 9. 링크 공유
  Future<String> createShareLink(String ideaId) async {
    final res = await _request(
      '/app/shares',
      method: 'POST',
      auth: true,
      body: {'ideaId': ideaId},
    );
    return res['shareUrl'] as String? ?? '';
  }

  // 10. 피드에 올리기
  Future<String> createPost(String ideaId) async {
    final res = await _request(
      '/app/posts',
      method: 'POST',
      auth: true,
      body: {'ideaId': ideaId},
    );
    return res['postId'] as String? ?? '';
  }

  // 11. 피드 목록 조회
  Future<List<PostItem>> getPosts({String sort = 'latest', int limit = 20}) async {
    final res = await _request(
      '/app/posts',
      method: 'GET',
      auth: true,
      queryParams: {'sort': sort, 'limit': limit},
    );
    final items = res['items'] as List<dynamic>? ?? [];
    return items.map((e) => PostItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  // 12. 피드 반응 (like, meh, dislike, null)
  Future<void> reactPost(String postId, String? kind) async {
    await _request(
      '/app/posts/$postId/reaction',
      method: 'PUT',
      auth: true,
      body: {'kind': kind},
    );
  }
}

