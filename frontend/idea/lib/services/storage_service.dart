import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/models.dart';

class SavedRecord {
  final String id; // timestamp or ideaId
  final String categoryName;
  final DateTime at;
  final IdeaItem idea;
  final GuideItem? guide;

  SavedRecord({
    required this.id,
    required this.categoryName,
    required this.at,
    required this.idea,
    this.guide,
  });

  factory SavedRecord.fromJson(Map<String, dynamic> json) {
    return SavedRecord(
      id: json['id'] as String? ?? '',
      categoryName: json['categoryName'] as String? ?? '',
      at: DateTime.tryParse(json['at'] as String? ?? '') ?? DateTime.now(),
      idea: IdeaItem.fromJson(json['idea'] as Map<String, dynamic>),
      guide: json['guide'] != null
          ? GuideItem.fromJson(json['guide'] as Map<String, dynamic>)
          : null,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'categoryName': categoryName,
        'at': at.toIso8601String(),
        'idea': idea.toJson(),
        'guide': guide?.toJson(),
      };
}

class StorageService extends ChangeNotifier {
  static final StorageService _instance = StorageService._internal();
  factory StorageService() => _instance;
  StorageService._internal();

  static const String _key = 'saved_idea_records';
  final List<SavedRecord> _records = [];
  bool _loaded = false;

  List<SavedRecord> get records => List.unmodifiable(_records);

  Future<void> init() async {
    if (_loaded) return;
    final prefs = await SharedPreferences.getInstance();
    final jsonList = prefs.getStringList(_key) ?? [];
    _records.clear();
    for (final str in jsonList) {
      try {
        _records.add(SavedRecord.fromJson(jsonDecode(str)));
      } catch (_) {}
    }
    _loaded = true;
    notifyListeners();
  }

  Future<void> save(IdeaItem idea, {GuideItem? guide}) async {
    await init();
    final record = SavedRecord(
      id: idea.id.isNotEmpty ? idea.id : DateTime.now().millisecondsSinceEpoch.toString(),
      categoryName: idea.categoryName,
      at: DateTime.now(),
      idea: idea,
      guide: guide,
    );
    _records.removeWhere((r) => r.id == record.id);
    _records.insert(0, record);
    await _persist();
    notifyListeners();
  }

  Future<void> remove(String id) async {
    await init();
    _records.removeWhere((r) => r.id == id);
    await _persist();
    notifyListeners();
  }

  bool isSaved(String id) {
    if (id.isEmpty) return false;
    return _records.any((r) => r.id == id);
  }

  Future<void> _persist() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonList = _records.map((r) => jsonEncode(r.toJson())).toList();
    await prefs.setStringList(_key, jsonList);
  }
}

