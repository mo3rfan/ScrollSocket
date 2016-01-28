#define PORT 40118
#define PROTOCOL_VERSION 1

#pragma pack(push)
#pragma pack(1)

#define EVENT_TYPE_MOTION 0
#define EVENT_TYPE_BUTTON 1

#define DRIVERNAME "ScrollSkt"

struct event_packet
{
	char signature[9];
	uint16_t version;
	uint8_t type;	/* EVENT_TYPE_... */
	struct {	/* required */
		uint16_t x, y;
	};

	struct {	
		uint16_t ox, oy;	
	};
};

#pragma pack(pop)
