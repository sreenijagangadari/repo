## Connection Screen UI Layout

```
┌──────────────────────────────────────────────┐
│                                              │
│            ┌──────────────────┐             │
│            │                  │             │
│            │   Connection     │             │
│            │                  │             │
│            │  ┌────────────┐  │             │
│            │  │            │  │             │
│            │  │ IP Address │  │             │
│            │  │            │  │             │
│            │  └────────────┘  │             │
│            │                  │             │
│            │  ┌────────────┐  │             │
│            │  │            │  │             │
│            │  │ Port Number│  │             │
│            │  │            │  │             │
│            │  └────────────┘  │             │
│            │                  │             │
│            │  ┌────────────┐  │             │
│            │  │  Connect   │  │             │
│            │  └────────────┘  │             │
│            │                  │             │
│            │  ℹ TCP connection │             │
│            │  with heartbeat   │             │
│            │  validation       │             │
│            └──────────────────┘             │
│                                              │
└──────────────────────────────────────────────┘

States:
1. Initial State:
   - IP: "192.168.1.100" (editable)
   - Port: "5760" (editable)
   - Button: "Connect" (enabled)

2. Connecting State:
   - Fields: disabled
   - Button: Shows spinner + "Connecting..." (disabled)

3. Success State:
   - Navigates to Main Screen
   - Connection screen removed from back stack

4. Error State:
   - Shows Alert Dialog:
     ┌─────────────────────┐
     │   ⚠ Connection Failed │
     │                     │
     │   [Error Message]   │
     │                     │
     │        [ OK ]       │
     └─────────────────────┘
   - After dismissing, returns to Initial State
```

## Color Scheme (Material Design 3)
- Primary: Used for heading and button
- Secondary: Used for info text
- Error: Used for error dialog icon
- Surface: Card background
- Background: Screen background

## Typography
- Heading: 32sp, Bold
- Text Fields: 16sp, Regular
- Button: 16sp, SemiBold
- Info: 12sp, Regular

## Spacing
- Card padding: 32dp
- Element spacing: 24dp
- Card elevation: 8dp
- Card corner radius: 16dp
- Button height: 56dp

