import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/app/router.dart';
import 'package:mtaafix_mobile/core/widgets/snackbars.dart';
import 'package:mtaafix_mobile/features/auth/application/auth_scope.dart';

/// Requests a password-reset link (FR-1.7). The backend answer is always the
/// same for every address — this screen mirrors that non-disclosure.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  bool _loading = false;
  String? _sentTo;

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);

    final auth = AuthScope.read(context);
    final success = await auth.forgotPassword(_emailController.text);
    if (!mounted) return;
    setState(() => _loading = false);

    if (success) {
      setState(() => _sentTo = _emailController.text.trim());
    } else {
      showAppSnack(
        context,
        auth.errorMessage ?? 'Could not request a reset link. Try again.',
        error: true,
      );
    }
  }

  void _backToSignIn() {
    Navigator.of(context).pushReplacementNamed(Routes.login);
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final sentTo = _sentTo;

    if (sentTo != null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Forgot password')),
        body: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.mark_email_read_rounded,
                  size: 72,
                  color: scheme.primary,
                ),
                const SizedBox(height: 24),
                Text(
                  'Check your inbox',
                  style: text.headlineSmall?.copyWith(
                    color: scheme.primary,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  'If an account exists for $sentTo, a password reset link is '
                  'on its way. Open the link on this device, or copy the '
                  'reset token from the email and continue below.',
                  textAlign: TextAlign.center,
                  style: text.bodyMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  'The link expires in 30 minutes and can only be used once.',
                  textAlign: TextAlign.center,
                  style: text.bodySmall?.copyWith(color: Colors.grey),
                ),
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: ElevatedButton(
                    onPressed: () => Navigator.pushReplacementNamed(
                      context,
                      Routes.resetPassword,
                    ),
                    child: const Text('I have a reset token'),
                  ),
                ),
                const SizedBox(height: 8),
                TextButton(
                  onPressed: _backToSignIn,
                  child: const Text('Back to sign in'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Forgot password')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.lock_reset_rounded,
                  size: 72,
                  color: scheme.primary,
                ),
                const SizedBox(height: 24),
                Text(
                  'Reset your password',
                  style: text.headlineSmall?.copyWith(
                    color: scheme.primary,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Enter the email you signed up with and we\'ll send you a '
                  'link to set a new password.',
                  textAlign: TextAlign.center,
                  style: text.bodyMedium,
                ),
                const SizedBox(height: 24),
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  autofillHints: const [AutofillHints.email],
                  decoration: const InputDecoration(
                    labelText: 'Email',
                    prefixIcon: Icon(Icons.email_rounded),
                  ),
                  validator: (value) {
                    final email = value?.trim() ?? '';
                    if (email.isEmpty) return 'Please enter your email';
                    if (!email.contains('@')) {
                      return 'Please enter a valid email';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: ElevatedButton(
                    onPressed: _loading ? null : _submit,
                    child: _loading
                        ? SizedBox(
                            height: 24,
                            width: 24,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: scheme.onPrimary,
                            ),
                          )
                        : const Text('Send reset link'),
                  ),
                ),
                const SizedBox(height: 8),
                TextButton(
                  onPressed: _backToSignIn,
                  child: const Text('Back to sign in'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
