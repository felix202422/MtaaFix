import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiClient {
  static final String _baseUrl = 'http://localhost:8080/api/v1';
  
  static http.Client? _client;
  static const String? _authToken = null; // Will be set after login

  factory ApiClient() => _instance;
  ApiClient._() {
    _client = http.Client();
  }

  static final ApiClient _instance = ApiClient._();

  http.Client? get client => _client;

  Future<Map<String, dynamic>> get(String endpoint) async {
    final response = await _client!.get(
      Uri.parse('$_baseUrl$endpoint'),
      headers: await _getHeaders(),
    );
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> post(String endpoint, dynamic data) async {
    final response = await _client!.post(
      Uri.parse('$_baseUrl$endpoint'),
      headers: await _getHeaders(),
      body: data is Map<String, dynamic> ? jsonEncode(data) : data,
    );
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> put(String endpoint, dynamic data) async {
    final response = await _client!.put(
      Uri.parse('$_baseUrl$endpoint'),
      headers: await _getHeaders(),
      body: data is Map<String, dynamic> ? jsonEncode(data) : data,
    );
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> delete(String endpoint) async {
    final response = await _client!.delete(
      Uri.parse('$_baseUrl$endpoint'),
      headers: await _getHeaders(),
    );
    return _handleResponse(response);
  }

  Future<Map<String, String>> _getHeaders() async {
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
  }

  Map<String, dynamic> _handleResponse(http.Response response) {
    final body = json.decode(response.body) as Map<String, dynamic>;
    
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return body;
    } else {
      throw Exception(body['message'] ?? 'Request failed');
    }
  }
}
