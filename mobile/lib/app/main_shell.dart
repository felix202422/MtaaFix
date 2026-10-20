import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/features/notifications/application/unread_count_scope.dart';
import 'package:mtaafix_mobile/features/notifications/presentation/notifications_screen.dart';
import 'package:mtaafix_mobile/features/profile/presentation/profile_screen.dart';
import 'package:mtaafix_mobile/features/reports/presentation/report_list_screen.dart';
import 'package:mtaafix_mobile/features/map/presentation/map_screen.dart';
import 'package:mtaafix_mobile/features/home/presentation/home_screen.dart';

/// Bottom-navigation shell (Page 30): Home, My Reports, Map, Notifications,
/// Profile — kept in an [IndexedStack] so tab state survives switching.
class MainShellScreen extends StatefulWidget {
  const MainShellScreen({super.key, this.initialIndex = 0});

  final int initialIndex;

  @override
  State<MainShellScreen> createState() => _MainShellScreenState();
}

class _MainShellScreenState extends State<MainShellScreen> {
  late int _index = widget.initialIndex;
  late final ValueNotifier<int> _activeTab = ValueNotifier<int>(widget.initialIndex);
  bool _unreadRefreshed = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Fetch the badge count once per shell mount (scope may be absent in
    // minimal test harnesses — [UnreadCountScope.read] is nullable).
    if (!_unreadRefreshed) {
      _unreadRefreshed = true;
      UnreadCountScope.read(context)?.refresh();
    }
  }

  @override
  void dispose() {
    _activeTab.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final unread = UnreadCountScope.maybeOf(context)?.count ?? 0;
    final unreadLabel = unread > 99 ? '99+' : '$unread';
    return Scaffold(
      body: MainShellTab(
        notifier: _activeTab,
        child: IndexedStack(
          index: _index,
          children: const [
            HomeScreen(),
            ReportListScreen(),
            MapScreen(),
            NotificationsScreen(),
            ProfileScreen(),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (index) {
          _activeTab.value = index;
          setState(() => _index = index);
        },
        destinations: [
          const NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home_rounded),
            label: 'Home',
          ),
          const NavigationDestination(
            icon: Icon(Icons.list_alt_outlined),
            selectedIcon: Icon(Icons.list_alt_rounded),
            label: 'Reports',
          ),
          const NavigationDestination(
            icon: Icon(Icons.map_outlined),
            selectedIcon: Icon(Icons.map_rounded),
            label: 'Map',
          ),
          NavigationDestination(
            icon: Badge(
              isLabelVisible: unread > 0,
              label: Text(unreadLabel),
              child: const Icon(Icons.notifications_outlined),
            ),
            selectedIcon: Badge(
              isLabelVisible: unread > 0,
              label: Text(unreadLabel),
              child: const Icon(Icons.notifications_rounded),
            ),
            label: 'Alerts',
          ),
          const NavigationDestination(
            icon: Icon(Icons.person_outline_rounded),
            selectedIcon: Icon(Icons.person_rounded),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}

/// Publishes which shell tab is active so data screens refresh when they are
/// re-selected (an [IndexedStack] alone would keep first-mount data forever —
/// e.g. a report submitted after login never appearing in My Reports).
class MainShellTab extends InheritedNotifier<ValueNotifier<int>> {
  const MainShellTab({
    super.key,
    required ValueNotifier<int> notifier,
    required super.child,
  }) : super(notifier: notifier);

  static const int home = 0;
  static const int reports = 1;
  static const int map = 2;
  static const int notifications = 3;
  static const int profile = 4;

  static ValueNotifier<int>? maybeOf(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<MainShellTab>()?.notifier;
}
