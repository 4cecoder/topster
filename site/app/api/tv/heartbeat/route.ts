import { NextResponse } from 'next/server';

// This would reference the same storage as devices/route.ts
// In a real app, use a shared store (Redis, database, etc.)

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { deviceId } = body;

    if (!deviceId) {
      return NextResponse.json(
        { error: 'Device ID required' },
        { status: 400 }
      );
    }

    // Update last seen timestamp
    // In production, update the device in your store
    // registeredDevices.get(deviceId).lastSeen = new Date();

    return NextResponse.json({ success: true, timestamp: Date.now() });
  } catch (error) {
    return NextResponse.json(
      { error: 'Invalid request' },
      { status: 400 }
    );
  }
}
