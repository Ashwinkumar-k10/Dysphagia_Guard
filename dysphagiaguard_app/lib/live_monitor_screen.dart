import 'package:flutter/material.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import 'main.dart';
import 'splash_screen.dart';

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
    appState.stopMonitoring();
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
              pw.Text('Emergency SOS Triggers: ${appState.sosEvents}'),
              pw.SizedBox(height: 20),
              pw.Text('Detailed event history included in offline exports.'),
            ],
          ),
        ),
      ),
    );

    await Printing.layoutPdf(onLayout: (PdfPageFormat format) async => pdf.save());
    
    if (mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (context) => const SplashScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    Color statusColor;
    if (appState.currentStatus == "SAFE") {
      statusColor = const Color(0xFF10B981);
    } else if (appState.currentStatus == "UNSAFE") {
      statusColor = const Color(0xFFEF4444);
    } else if (appState.currentStatus == "SOS") {
      statusColor = const Color(0xFFF59E0B);
    } else {
      statusColor = const Color(0xFF151C2C);
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text("Live Monitor", style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.stop_circle, color: Color(0xFFEF4444)),
            onPressed: _endSessionAndGeneratePDF,
            tooltip: 'End Session & Report',
          )
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: appState.triggerSOS,
        backgroundColor: const Color(0xFFF59E0B),
        child: const Icon(Icons.sos, color: Colors.white, size: 30),
        tooltip: 'Emergency SOS',
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              height: 220,
              width: double.infinity,
              decoration: BoxDecoration(
                color: statusColor,
                borderRadius: BorderRadius.circular(24),
                boxShadow: appState.currentStatus != "IDLE" 
                    ? [BoxShadow(color: statusColor.withOpacity(0.5), blurRadius: 20, spreadRadius: 5)] 
                    : [],
                border: (appState.currentStatus == "UNSAFE" || appState.currentStatus == "SOS")
                    ? Border.all(color: Colors.white, width: 4) 
                    : null,
              ),
              child: Center(
                child: Text(
                  appState.currentStatus,
                  style: const TextStyle(fontSize: 48, fontWeight: FontWeight.w900, color: Colors.white, letterSpacing: 2),
                ),
              ),
            ),
            const SizedBox(height: 32),
            Container(
              height: 120,
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.black26,
                borderRadius: BorderRadius.circular(12),
              ),
              alignment: Alignment.bottomCenter,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 100),
                  height: 120 * appState.liveMicValue,
                  width: double.infinity,
                  color: const Color(0xFF38BDF8),
                ),
              ),
            ),
            const SizedBox(height: 16),
            const Text("Live Acoustic Waveform (Mic)", style: TextStyle(color: Colors.white54, fontWeight: FontWeight.w500)),
            const SizedBox(height: 32),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                Expanded(child: _StatCard("Total", "${appState.totalSwallows}")),
                const SizedBox(width: 16),
                Expanded(child: _StatCard("Unsafe", "${appState.unsafeSwallows}", alertColor: const Color(0xFFEF4444), isAlert: appState.unsafeSwallows > 0)),
                const SizedBox(width: 16),
                Expanded(child: _StatCard("SOS", "${appState.sosEvents}", alertColor: const Color(0xFFF59E0B), isAlert: appState.sosEvents > 0)),
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
  final Color? alertColor;

  const _StatCard(this.label, this.value, {this.isAlert = false, this.alertColor, Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF151C2C),
        borderRadius: BorderRadius.circular(16),
        border: isAlert ? Border.all(color: alertColor!, width: 2) : null,
      ),
      child: Column(
        children: [
          Text(label, style: const TextStyle(color: Colors.white70, fontSize: 14)),
          const SizedBox(height: 8),
          Text(
            value,
            style: TextStyle(
              fontSize: 28, 
              fontWeight: FontWeight.bold, 
              color: isAlert ? alertColor : Colors.white
            ),
          ),
        ],
      ),
    );
  }
}
