class User {
  final String id;
  final String email;
  final String fullName;
  final String? mobile;
  final String role;
  final String? organisationId;
  final bool active;

  User({
    required this.id,
    required this.email,
    required this.fullName,
    this.mobile,
    required this.role,
    this.organisationId,
    required this.active,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] as String,
      email: json['email'] as String,
      fullName: json['fullName'] as String,
      mobile: json['mobile'] as String?,
      role: json['role'] as String,
      organisationId: json['organisationId'] as String?,
      active: json['active'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'fullName': fullName,
      'mobile': mobile,
      'role': role,
      'organisationId': organisationId,
      'active': active,
    };
  }

  bool get isCitizen => role == 'CITIZEN';
  bool get isModerator => role == 'MODERATOR';
  bool get isAdmin => role == 'ADMINISTRATOR';
  bool get isFieldWorker => role == 'FIELD_WORKER';
  bool get isOrgAdmin => role == 'ORGANIZATION_ADMIN';
}
