#ifndef __IO_CONFIG_H__
#define __IO_CONFIG_H__

#include <ble.h>
#include <stdbool.h>

#define RESET_BUTTON			0
#define STATUS_LED				21
#define VALUE_LED_1				6


void gpio_init(void);
void setMeshValueTo(uint8_t handle, uint8_t value);
	
#endif 
