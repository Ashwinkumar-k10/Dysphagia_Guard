import 'package:flutter/material.dart';

class AegisMedicalScreen extends StatefulWidget {
  const AegisMedicalScreen({Key? key}) : super(key: key);

  @override
  State<AegisMedicalScreen> createState() => _AegisMedicalScreenState();
}

class _AegisMedicalScreenState extends State<AegisMedicalScreen> {
  final TextEditingController _controller = TextEditingController();
  final List<Map<String, String>> _messages = [
    {
      "sender": "aegis",
      "text": "Hello! I am Aegis Medical, your AI assistant. How can I help you with DysphagiaGuard today?"
    }
  ];

  void _sendMessage() {
    if (_controller.text.trim().isEmpty) return;

    final userMessage = _controller.text.trim();
    setState(() {
      _messages.add({"sender": "user", "text": userMessage});
    });
    _controller.clear();

    // Simulate AI thinking and responding
    Future.delayed(const Duration(seconds: 1), () {
      setState(() {
        if (userMessage.toLowerCase().contains("sos") || userMessage.toLowerCase().contains("emergency")) {
          _messages.add({
            "sender": "aegis",
            "text": "Emergency triggered! An SOS alert has been recorded and medical professionals have been notified."
          });
        } else if (userMessage.toLowerCase().contains("report") || userMessage.toLowerCase().contains("history")) {
          _messages.add({
            "sender": "aegis",
            "text": "You can view your detailed report in the 'History' tab or end the current monitoring session to generate a PDF report."
          });
        } else {
          _messages.add({
            "sender": "aegis",
            "text": "I am analyzing your query. Please note that I am an AI assistant and you should consult a doctor for serious medical concerns."
          });
        }
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: const [
            Icon(Icons.smart_toy, color: Color(0xFF38BDF8)),
            SizedBox(width: 8),
            Text("Aegis Medical AI", style: TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final isUser = _messages[index]["sender"] == "user";
                return Align(
                  alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    decoration: BoxDecoration(
                      color: isUser ? const Color(0xFF38BDF8) : const Color(0xFF151C2C),
                      borderRadius: BorderRadius.only(
                        topLeft: const Radius.circular(16),
                        topRight: const Radius.circular(16),
                        bottomLeft: Radius.circular(isUser ? 16 : 0),
                        bottomRight: Radius.circular(isUser ? 0 : 16),
                      ),
                    ),
                    child: Text(
                      _messages[index]["text"]!,
                      style: const TextStyle(fontSize: 16, color: Colors.white),
                    ),
                  ),
                );
              },
            ),
          ),
          Container(
            padding: const EdgeInsets.all(16),
            color: const Color(0xFF151C2C),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    onSubmitted: (_) => _sendMessage(),
                    decoration: InputDecoration(
                      hintText: "Ask Aegis Medical...",
                      hintStyle: const TextStyle(color: Colors.white54),
                      filled: true,
                      fillColor: const Color(0xFF0A0E17),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(24),
                        borderSide: BorderSide.none,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                CircleAvatar(
                  backgroundColor: const Color(0xFF38BDF8),
                  child: IconButton(
                    icon: const Icon(Icons.send, color: Colors.white),
                    onPressed: _sendMessage,
                  ),
                )
              ],
            ),
          )
        ],
      ),
    );
  }
}
