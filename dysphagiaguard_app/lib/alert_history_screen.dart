import 'package:flutter/material.dart';
import 'main.dart';

class AlertHistoryScreen extends StatefulWidget {
  const AlertHistoryScreen({Key? key}) : super(key: key);

  @override
  State<AlertHistoryScreen> createState() => _AlertHistoryScreenState();
}

class _AlertHistoryScreenState extends State<AlertHistoryScreen> {
  @override
  void initState() {
    super.initState();
    appState.addListener(_update);
  }

  @override
  void dispose() {
    appState.removeListener(_update);
    super.dispose();
  }

  void _update() => setState(() {});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Alert History", style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: appState.events.isEmpty 
        ? const Center(child: Text("No events recorded yet.", style: TextStyle(color: Colors.white54)))
        : ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: appState.events.length,
            itemBuilder: (context, index) {
              final event = appState.events[index];
              Color color;
              IconData icon;
              
              if (event.classification == "SAFE") {
                color = const Color(0xFF10B981);
                icon = Icons.check_circle;
              } else if (event.classification == "UNSAFE") {
                color = const Color(0xFFEF4444);
                icon = Icons.warning;
              } else {
                color = const Color(0xFFF59E0B);
                icon = Icons.sos;
              }
              
              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  leading: CircleAvatar(
                    backgroundColor: color.withOpacity(0.2),
                    child: Icon(icon, color: color),
                  ),
                  title: Text(
                    event.classification,
                    style: TextStyle(fontWeight: FontWeight.bold, color: color, fontSize: 18),
                  ),
                  subtitle: Padding(
                    padding: const EdgeInsets.only(top: 4.0),
                    child: Text("Confidence: ${(event.confidence * 100).toInt()}%"),
                  ),
                  trailing: Text(
                    "${event.timestamp.hour.toString().padLeft(2, '0')}:${event.timestamp.minute.toString().padLeft(2, '0')}:${event.timestamp.second.toString().padLeft(2, '0')}",
                    style: const TextStyle(color: Colors.white54, fontWeight: FontWeight.w500),
                  ),
                ),
              );
            },
          ),
    );
  }
}
