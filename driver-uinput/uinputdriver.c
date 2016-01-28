
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <limits.h>
#include <arpa/inet.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <stdint.h>
#include "protocol.h"

#define die(str, args...) { \
	perror(str); \
	exit(EXIT_FAILURE); \
}


int udp_socket;


void init_device(int fd)
{
	struct uinput_user_dev uidev;

	if (ioctl(fd, UI_SET_EVBIT, EV_REL) < 0)
        die("error: ioctl UI_SET_RELBIT EV_REL");
    if (ioctl(fd, UI_SET_RELBIT, REL_WHEEL) < 0)
        die("error: ioctl UI_SET_RELBIT REL_WHEEL");
    if (ioctl(fd, UI_SET_RELBIT, REL_HWHEEL) < 0)
        die("error: ioctl UI_SET_RELBIT REL_HWHEEL");

	if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0)
		die("error: ioctl UI_SET_EVBIT EV_KEY");
	if (ioctl(fd, UI_SET_KEYBIT, BTN_TOUCH) < 0)
    	die("error: ioctl UI_SET_KEYBIT");

    if (ioctl(fd, UI_SET_EVBIT, EV_ABS) < 0)
        die("error: ioctl UI_SET_EVBIT EV_ABS");
    if (ioctl(fd, UI_SET_ABSBIT, ABS_X) < 0)
        die("error: ioctl UI_SETEVBIT ABS_X");
    if (ioctl(fd, UI_SET_ABSBIT, ABS_Y) < 0)
        die("error: ioctl UI_SETEVBIT ABS_Y");
   
	memset(&uidev, 0, sizeof(uidev));
	snprintf(uidev.name, UINPUT_MAX_NAME_SIZE, DRIVERNAME);
	uidev.id.bustype = BUS_VIRTUAL;
	uidev.id.vendor  = 0x1;
	uidev.id.product = 0x1;
	uidev.id.version = 1;

    uidev.absmin[ABS_X] = 0;
    uidev.absmax[ABS_X] = UINT16_MAX;
    uidev.absmin[ABS_Y] = 0;
    uidev.absmax[ABS_Y] = UINT16_MAX;

    if (write(fd, &uidev, sizeof(uidev)) < 0)
		die("error: write");

	if (ioctl(fd, UI_DEV_CREATE) < 0)
		die("error: ioctl");
}

int prepare_socket()
{
	int s;
	struct sockaddr_in addr;

	if ((s = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == -1)
		die("error: prepare_socket()");

	bzero(&addr, sizeof(struct sockaddr_in));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(PORT);
	addr.sin_addr.s_addr = htonl(INADDR_ANY);

	if (bind(s, (struct sockaddr *)&addr, sizeof(addr)) == -1)
		die("error: prepare_socket()");

	return s;
}

void send_event(int device, int type, int code, int value)
{
	struct input_event ev;
	ev.type = type;
	ev.code = code;
	ev.value = value;
	if (write(device, &ev, sizeof(ev)) < 0)
		die("error: write()");
}

void quit(int signal) {
	close(udp_socket);
}


int main(void)
{
	int device;
	struct event_packet ev_pkt;

	if ((device = open("/dev/uinput", O_WRONLY | O_NONBLOCK)) < 0)
		die("error: open");

	init_device(device);
	udp_socket = prepare_socket();

	printf("%s driver (protocol version %u) is ready and listening on 0.0.0.0:%u (UDP)\n"
		"Hint: Make sure that this port is not blocked by your firewall.\n", DRIVERNAME, PROTOCOL_VERSION, PORT);

	signal(SIGINT, quit);
	signal(SIGTERM, quit);
    ev_pkt.oy = 0;
	while (recv(udp_socket, &ev_pkt, sizeof(ev_pkt), 0) >= 9) {		// every packet has at least 9 bytes
		printf("."); fflush(0);

		if (memcmp(ev_pkt.signature, DRIVERNAME, 9) != 0) {
			fprintf(stderr, "\nGot unknown packet on port %i, ignoring\n", PORT);
			continue;
		}
		ev_pkt.version = ntohs(ev_pkt.version);
		if (ev_pkt.version != PROTOCOL_VERSION) {
			fprintf(stderr, "\n%s app speaks protocol version %i but driver speaks version %i, please update\n",
				DRIVERNAME, ev_pkt.version, PROTOCOL_VERSION);
			break;
		}
        ev_pkt.x = ntohs(ev_pkt.x);
        ev_pkt.y = ntohs(ev_pkt.y);
        
		switch (ev_pkt.type) {
			case EVENT_TYPE_MOTION:
                send_event(device, EV_SYN, SYN_REPORT, 1);
                if (ev_pkt.oy > ev_pkt.y)
                    send_event(device, EV_REL, REL_WHEEL, -ev_pkt.y/ev_pkt.y);
                else
                    send_event(device, EV_REL, REL_WHEEL, ev_pkt.y/ev_pkt.y);
                ev_pkt.oy = ev_pkt.y;
				break;
			case EVENT_TYPE_BUTTON:
				break;

		}
	}
	close(udp_socket);

	printf("\nRemoving %s from device list\n", DRIVERNAME);
	ioctl(device, UI_DEV_DESTROY);
	close(device);

	printf("\n%s driver shut down gracefully\n", DRIVERNAME);
	return 0;
}
