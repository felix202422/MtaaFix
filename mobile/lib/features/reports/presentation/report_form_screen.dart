import 'package:flutter/material.dart';

import 'package:mtaafix_mobile/core/widgets/snackbars.dart';

class ReportFormScreen extends StatelessWidget {
  const ReportFormScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Report Issue')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Report Location',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 8),
            Container(
              height: 200,
              decoration: BoxDecoration(
                color: colors.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Center(
                child: Text('Map View'),
              ),
            ),
            const SizedBox(height: 24),
            DropdownButtonFormField<String>(
              decoration: const InputDecoration(
                labelText: 'Category',
              ),
              items: const [
                DropdownMenuItem(value: 'POTHOLE', child: Text('Pothole')),
                DropdownMenuItem(
                  value: 'LIGHT',
                  child: Text('Broken Streetlight'),
                ),
                DropdownMenuItem(
                  value: 'DAMAGE',
                  child: Text('Damaged Road'),
                ),
                DropdownMenuItem(
                  value: 'GARBAGE',
                  child: Text('Garbage Accumulation'),
                ),
                DropdownMenuItem(
                  value: 'DRAIN',
                  child: Text('Blocked Drainage'),
                ),
                DropdownMenuItem(value: 'WATER', child: Text('Water Leak')),
                DropdownMenuItem(value: 'FLOOD', child: Text('Flooding')),
                DropdownMenuItem(
                  value: 'WRK',
                  child: Text('Damaged Infrastructure'),
                ),
                DropdownMenuItem(value: 'TREE', child: Text('Fallen Tree')),
                DropdownMenuItem(
                  value: 'ELEC',
                  child: Text('Damaged Electrical'),
                ),
                DropdownMenuItem(
                  value: 'SAFETY',
                  child: Text('Unsafe Area'),
                ),
                DropdownMenuItem(value: 'OTHER', child: Text('Other')),
              ],
              onChanged: (value) {},
            ),
            const SizedBox(height: 16),
            TextFormField(
              decoration: const InputDecoration(
                labelText: 'Title',
              ),
              maxLines: 1,
            ),
            const SizedBox(height: 16),
            TextFormField(
              decoration: const InputDecoration(
                labelText: 'Description',
              ),
              maxLines: 4,
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              icon: const Icon(Icons.add_photo_alternate_rounded),
              label: const Text('Add Photo/Video'),
              onPressed: () {
                showAppSnack(
                  context,
                  'Photo capture arrives with the mobile reporting phase.',
                );
              },
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: () {
                  showAppSnack(
                    context,
                    'Report submission arrives with the mobile reporting phase.',
                  );
                  Navigator.pop(context);
                },
                child: const Text('Submit Report'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
