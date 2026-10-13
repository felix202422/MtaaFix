import 'package:flutter/material.dart';

class ReportFormScreen extends StatelessWidget {
  const ReportFormScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Report Issue'),
        backgroundColor: Colors.orange.shade600,
      ),
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
                color: Colors.grey[200],
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Center(
                child: Text('Map View'),
              ),
            ),
            const SizedBox(height: 24),
            DropdownButtonFormField<String>(
              decoration: const InputDecoration(
                labelText: 'Subject',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'POTHOLE', child: Text('Pothole')),
                DropdownMenuItem(value: 'LIGHT', child: Text('Broken Streetlight')),
                DropdownMenuItem(value: 'DAMAGE', child: Text('Damaged Road')),
                DropdownMenuItem(value: 'GARBAGE', child: Text('Garbage Accumulation')),
                DropdownMenuItem(value: 'DRAIN', child: Text('Blocked Drainage')),
                DropdownMenuItem(value: 'WATER', child: Text('Water Leak')),
                DropdownMenuItem(value: 'FLOOD', child: Text('Flooding')),
                DropdownMenuItem(value: 'WRK', child: Text('Damaged Infrastructure')),
                DropdownMenuItem(value: 'TREE', child: Text('Fallen Tree')),
                DropdownMenuItem(value: 'ELEC', child: Text('Damaged Electrical')),
                DropdownMenuItem(value: 'SAFETY', child: Text('Unsafe Area')),
                DropdownMenuItem(value: 'OTHER', child: Text('Other')),
              ],
              onChanged: (value) {},
            ),
            const SizedBox(height: 16),
            TextFormField(
              decoration: const InputDecoration(
                labelText: 'Title',
                border: OutlineInputBorder(),
              ),
              maxLines: 1,
            ),
            const SizedBox(height: 16),
            TextFormField(
              decoration: const InputDecoration(
                labelText: 'Description',
                border: OutlineInputBorder(),
              ),
              maxLines: 4,
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              icon: const Icon(Icons.add_photo_alternate_rounded),
              label: const Text('Add Photo/Video'),
              onPressed: () {},
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: () {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Report submitted')),
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
