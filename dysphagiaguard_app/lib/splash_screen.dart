import 'package:flutter/material.dart';
import 'patient_setup_screen.dart';

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
        MaterialPageRoute(builder: (context) => const PatientSetupScreen()),
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF0A0E17), Color(0xFF151C2C)],
          ),
        ),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: const [
              Icon(Icons.monitor_heart_outlined, size: 100, color: Color(0xFF38BDF8)),
              SizedBox(height: 24),
              Text(
                "DysphagiaGuard",
                style: TextStyle(
                  fontSize: 32, 
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.2,
                ),
              ),
              SizedBox(height: 8),
              Text(
                "Connecting to device...",
                style: TextStyle(color: Colors.white54, fontSize: 16),
              ),
              SizedBox(height: 48),
              CircularProgressIndicator(color: Color(0xFF38BDF8)),
            ],
          ),
        ),
      ),
    );
  }
}
