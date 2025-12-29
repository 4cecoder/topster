import { NextResponse } from 'next/server';
import type { TVDevice } from '@/lib/types';

// In-memory storage for TV devices (in production, use Redis or a database)
const registeredDevices = new Map<string, TVDevice>();

export async function GET() {
  const devices = Array.from(registeredDevices.values()).map(device => ({
    ...device,
    online: isDeviceOnline(device),
  }));

  return NextResponse.json({ devices });
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { id, name, ip, port } = body;

    if (!id || !name || !ip || !port) {
      return NextResponse.json(
        { error: 'Missing required fields' },
        { status: 400 }
      );
    }

    const device: TVDevice = {
      id,
      name,
      ip,
      port,
      online: true,
      lastSeen: new Date(),
    };

    registeredDevices.set(id, device);

    return NextResponse.json({ success: true, device });
  } catch (error) {
    return NextResponse.json(
      { error: 'Invalid request body' },
      { status: 400 }
    );
  }
}

export async function DELETE(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get('id');

    if (!id) {
      return NextResponse.json(
        { error: 'Device ID required' },
        { status: 400 }
      );
    }

    registeredDevices.delete(id);

    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json(
      { error: 'Failed to unregister device' },
      { status: 500 }
    );
  }
}

function isDeviceOnline(device: TVDevice): boolean {
  if (!device.lastSeen) return false;
  const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
  return device.lastSeen.getTime() > fiveMinutesAgo;
}
