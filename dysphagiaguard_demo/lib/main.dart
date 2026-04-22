import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

void main() {
  runApp(const DysphagiaGuardDemoApp());
}

class DysphagiaGuardDemoApp extends StatelessWidget {
  const DysphagiaGuardDemoApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'DysphagiaGuard Demo',
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0D1B2A),
        cardColor: const Color(0xFF1A2B3C),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF00B4D8),
          secondary: Color(0xFF2ECC71),
          error: Color(0xFFE74C3C),
          surface: Color(0xFF1A2B3C),
          background: Color(0xFF0D1B2A),
        ),
      ),
      home: const SplashScreen(),
    );
  }
}

class SwallowEvent {
  final String classification;
  final DateTime timestamp;
  final double confidence;

  SwallowEvent(this.classification, this.timestamp, this.confidence);
}

class AppState extends ChangeNotifier {
  List<SwallowEvent> events = [];
  String currentStatus = "IDLE";
  int totalSwallows = 0;
  int unsafeSwallows = 0;
  double liveMicValue = 0.0;
  double liveImuValue = 0.0;

  Timer? _safeTimer;
  Timer? _unsafeTimer;
  Timer? _waveformTimer;

  void startDemo() {
    _waveformTimer = Timer.periodic(const Duration(milliseconds: 50), (timer) {
      liveMicValue = Random().nextDouble() * 0.1;
      liveImuValue = Random().nextDouble() * 0.1;
      notifyListeners();
    });

    _safeTimer = Timer.periodic(const Duration(seconds: 4), (timer) {
      // Don't fire SAFE if it aligns with the 12s UNSAFE to avoid clash
      if (timer.tick % 3 != 0) {
        _fireEvent("SAFE");
      }
    });

    _unsafeTimer = Timer.periodic(const Duration(seconds: 12), (timer) {
      _fireEvent("UNSAFE");
    });
  }

  void _fireEvent(String type) {
    currentStatus = type;
    totalSwallows++;
    if (type == "UNSAFE") unsafeSwallows++;
    events.insert(0, SwallowEvent(type, DateTime.now(), 0.85 + Random().nextDouble() * 0.1));
    
    // Animate waveform spike
    liveMicValue = type == "UNSAFE" ? 0.9 : 0.6;
    liveImuValue = type == "UNSAFE" ? 0.9 : 0.6;
    
    notifyListeners();

    Future.delayed(const Duration(seconds: 1), () {
      if (currentStatus == type) {
        currentStatus = "IDLE";
        notifyListeners();
      }
    });
  }

  void stopDemo() {
    _safeTimer?.cancel();
    _unsafeTimer?.cancel();
    _waveformTimer?.cancel();
  }
}

final appState = AppState();

class SplashScreen extends StatefulWidget {
  const SplashScreen({Key? key}) : super(key: key);

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    Future.delayed(const Duration(seconds: 2), () {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (context) => const MainLayout()),
      );
      if (kIsWeb) {
        appState.startDemo();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: const [
            Icon(Icons.monitor_heart, size: 80, color: Color(0xFF00B4D8)),
            SizedBox(height: 20),
            Text("DysphagiaGuard Web Demo", style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
            SizedBox(height: 20),
            CircularProgressIndicator(color: Color(0xFF00B4D8)),
          ],
        ),
      ),
    );
  }
}

class MainLayout extends StatefulWidget {
  const MainLayout({Key? key}) : super(key: key);

  @override
  State<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends State<MainLayout> {
  int _currentIndex = 0;

  final List<Widget> _screens = [
    const LiveMonitorScreen(),
    const AlertHistoryScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (idx) => setState(() => _currentIndex = idx),
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.monitor_heart), label: "Monitor"),
          BottomNavigationBarItem(icon: Icon(Icons.warning), label: "Alerts"),
        ],
      ),
    );
  }
}

class LiveMonitorScreen extends StatefulWidget {
  const LiveMonitorScreen({Key? key}) : super(key: key);

  @override
  State<LiveMonitorScreen> createState() => _LiveMonitorScreenState();
}

class _LiveMonitorScreenState extends State<LiveMonitorScreen> {
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

  Future<void> _endSessionAndGeneratePDF() async {
    appState.stopDemo();
    final pdf = pw.Document();
    
    pdf.addPage(
      pw.Page(
        build: (pw.Context context) => pw.Center(
          child: pw.Column(
            mainAxisAlignment: pw.MainAxisAlignment.center,
            children: [
              pw.Text('DysphagiaGuard Session Report', style: pw.TextStyle(fontSize: 24, fontWeight: pw.FontWeight.bold)),
              pw.SizedBox(height: 20),
              pw.Text('Total Swallows: ${appState.totalSwallows}'),
              pw.Text('Unsafe Events: ${appState.unsafeSwallows}'),
              pw.SizedBox(height: 20),
              pw.Text('Detailed event history included in offline exports.'),
            ],
          ),
        ),
      ),
    );

    await Printing.layoutPdf(onLayout: (PdfPageFormat format) async => pdf.save());
  }

  @override
  Widget build(BuildContext context) {
    final statusColor = appState.currentStatus == "SAFE" 
        ? const Color(0xFF2ECC71) 
        : appState.currentStatus == "UNSAFE" 
            ? const Color(0xFFE74C3C) 
            : const Color(0xFF1A2B3C);

    return Scaffold(
      appBar: AppBar(
        title: const Text("Live Monitor"),
        actions: [
          IconButton(
            icon: const Icon(Icons.stop_circle, color: Colors.redAccent),
            onPressed: _endSessionAndGeneratePDF,
            tooltip: 'End Session & PDF',
          )
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              height: 200,
              width: double.infinity,
              decoration: BoxDecoration(
                color: statusColor,
                borderRadius: BorderRadius.circular(24),
                border: appState.currentStatus == "UNSAFE" ? Border.all(color: Colors.white, width: 4) : null,
              ),
              child: Center(
                child: Text(
                  appState.currentStatus,
                  style: const TextStyle(fontSize: 48, fontWeight: FontWeight.bold, color: Colors.white),
                ),
              ),
            ),
            const SizedBox(height: 32),
            Container(
              height: 100,
              width: double.infinity,
              color: Colors.black26,
              alignment: Alignment.bottomCenter,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 100),
                height: 100 * appState.liveMicValue,
                width: double.infinity,
                color: const Color(0xFF00B4D8),
              ),
            ),
            const SizedBox(height: 16),
            const Text("Live Acoustic Waveform (Mic)"),
            const SizedBox(height: 32),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                _StatCard("Total", "${appState.totalSwallows}"),
                _StatCard("Unsafe", "${appState.unsafeSwallows}", isAlert: appState.unsafeSwallows > 0),
              ],
            )
          ],
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final String value;
  final bool isAlert;

  const _StatCard(this.label, this.value, {this.isAlert = false, Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1A2B3C),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        children: [
          Text(label, style: const TextStyle(color: Colors.white70)),
          const SizedBox(height: 8),
          Text(
            value,
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: isAlert ? const Color(0xFFE74C3C) : Colors.white),
          ),
        ],
      ),
    );
  }
}

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
      appBar: AppBar(title: const Text("Alert History")),
      body: ListView.builder(
        itemCount: appState.events.length,
        itemBuilder: (context, index) {
          final event = appState.events[index];
          final color = event.classification == "SAFE" ? const Color(0xFF2ECC71) : const Color(0xFFE74C3C);
          return Card(
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: ListTile(
              leading: Container(width: 6, color: color),
              title: Text(event.classification),
              subtitle: Text("Confidence: ${(event.confidence * 100).toInt()}%"),
              trailing: Text("${event.timestamp.hour}:${event.timestamp.minute}:${event.timestamp.second}"),
            ),
          );
        },
      ),
    );
  }
}
