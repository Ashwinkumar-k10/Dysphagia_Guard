import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'splash_screen.dart';
import 'live_monitor_screen.dart';
import 'alert_history_screen.dart';
import 'aegis_medical_screen.dart';
import 'patient_setup_screen.dart';

void main() {
  runApp(const DysphagiaGuardApp());
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
  int sosEvents = 0;
  
  double liveMicValue = 0.0;
  double liveImuValue = 0.0;

  Timer? _safeTimer;
  Timer? _unsafeTimer;
  Timer? _waveformTimer;

  void startMonitoring() {
    _waveformTimer = Timer.periodic(const Duration(milliseconds: 50), (timer) {
      liveMicValue = Random().nextDouble() * 0.1;
      liveImuValue = Random().nextDouble() * 0.1;
      notifyListeners();
    });

    _safeTimer = Timer.periodic(const Duration(seconds: 5), (timer) {
      if (timer.tick % 3 != 0) {
        _fireEvent("SAFE");
      }
    });

    _unsafeTimer = Timer.periodic(const Duration(seconds: 15), (timer) {
      _fireEvent("UNSAFE");
    });
  }

  void triggerSOS() {
    _fireEvent("SOS");
  }

  void _fireEvent(String type) {
    currentStatus = type;
    if (type == "SAFE" || type == "UNSAFE") {
      totalSwallows++;
    }
    if (type == "UNSAFE") unsafeSwallows++;
    if (type == "SOS") sosEvents++;
    
    events.insert(0, SwallowEvent(type, DateTime.now(), 0.85 + Random().nextDouble() * 0.1));
    
    // Animate waveform spike
    if (type == "UNSAFE" || type == "SOS") {
      liveMicValue = 0.9;
      liveImuValue = 0.9;
    } else {
      liveMicValue = 0.6;
      liveImuValue = 0.6;
    }
    
    notifyListeners();

    Future.delayed(const Duration(seconds: 1), () {
      if (currentStatus == type) {
        currentStatus = "IDLE";
        notifyListeners();
      }
    });
  }

  void stopMonitoring() {
    _safeTimer?.cancel();
    _unsafeTimer?.cancel();
    _waveformTimer?.cancel();
  }
}

final appState = AppState();

class DysphagiaGuardApp extends StatelessWidget {
  const DysphagiaGuardApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'DysphagiaGuard',
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0A0E17),
        cardColor: const Color(0xFF151C2C),
        textTheme: GoogleFonts.interTextTheme(Theme.of(context).textTheme).apply(
          bodyColor: Colors.white,
          displayColor: Colors.white,
        ),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF38BDF8),
          secondary: Color(0xFF10B981),
          error: Color(0xFFEF4444),
          surface: Color(0xFF151C2C),
          background: Color(0xFF0A0E17),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF0A0E17),
          elevation: 0,
        ),
        bottomNavigationBarTheme: const BottomNavigationBarThemeData(
          backgroundColor: Color(0xFF151C2C),
          selectedItemColor: Color(0xFF38BDF8),
          unselectedItemColor: Colors.grey,
        )
      ),
      home: const SplashScreen(),
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
    const AegisMedicalScreen(),
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
          BottomNavigationBarItem(icon: Icon(Icons.history), label: "History"),
          BottomNavigationBarItem(icon: Icon(Icons.smart_toy), label: "Aegis AI"),
        ],
      ),
    );
  }
}
