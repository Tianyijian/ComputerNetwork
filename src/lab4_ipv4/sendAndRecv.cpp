/*
* THIS FILE IS FOR IP TEST
*/
// system support
#include "sysInclude.h"

extern void ip_DiscardPkt(char* pBuffer,int type);

extern void ip_SendtoLower(char*pBuffer,int length);

extern void ip_SendtoUp(char *pBuffer,int length);

extern unsigned int getIpv4Address();

// implemented by students

int stud_ip_recv(char *pBuffer,unsigned short length)
{
	int version = pBuffer[0]/16;    //IP版本号
	if (version != 4)    //检查IP
	{
		printf("Wrong version is %d\n", version);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_VERSION_ERROR);
		return 1;
	}
	int IHL = pBuffer[0]%16;     //IP Head length
	if (IHL< 5)         //检查IHL
	{
		printf("Wrong IHL is %d\n", IHL);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_HEADLEN_ERROR);
		return 1;
	}

 	int ttl = (int)pBuffer[8]; 			//TTL
	if (ttl == 0)   	  //检查TTL
	{
		printf("Wrong ttl is %d\n", ttl);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_TTL_ERROR);
		return 1;
	}
	int dstAddr = ntohl(*(unsigned int*)(pBuffer + 16));    //目的地址 转为主机字节序
	if (dstAddr != getIpv4Address() && dstAddr != 0xffff)
	{
		printf("Wrong dstAddr is 0x%x, localAddr is 0x%x\n", dstAddr,getIpv4Address());
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_DESTINATION_ERROR);    
        	return 1; 
	}
    	unsigned int sum = 0;	//计算校验和
    	for (int i = 0; i < IHL * 2; i++)
    	{
		if (i != 5)		//跳过校验和字段
		{
			sum += pBuffer[i*2]<<8 | pBuffer[i*2+1];
			sum %= 0xffff;
		}

    	}
	unsigned short calcCheckSum = ~(unsigned short)sum;
	unsigned short checkSum =  ntohs(*(unsigned short*)(pBuffer+10));		//取出校验和字段
	if (calcCheckSum != checkSum)		//计算的校验和与校验和字段比较
	{
		printf("Wrong checksum is 0x%x, should be 0x%x\n", checkSum, calcCheckSum);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_CHECKSUM_ERROR);
		return 1;
	}
	ip_SendtoUp(pBuffer,length);	//发送数据包给上层
	return 0;
}

int stud_ip_Upsend(char *pBuffer,unsigned short len,unsigned int srcAddr,
				   unsigned int dstAddr,byte protocol,byte ttl)
{
    	char *buffer = (char *)malloc((len + 20) * sizeof(char));    //申请空间
    	memset(buffer, 0, len+20);  //初始化空间为零
    	buffer[0] = 0x45;           //版本号以及IHL
	unsigned short totalLength = htons(len+20);	//总长度 转为网络字节序
    	memcpy(buffer+2, &totalLength, sizeof(unsigned short));  //总长度赋值
    	buffer[8] = ttl;            //TTL
    	buffer[9] = protocol;       //协议
	unsigned int srcAddress = htonl(srcAddr);		//源地址 转为网络字节序
    	memcpy(buffer+12, &srcAddress, sizeof(unsigned int));  //源地址赋值
	unsigned int dstAddress = htonl(dstAddr);		//目的地址 转为网络字节序
    	memcpy(buffer+16, &dstAddress, sizeof(unsigned int));  //目的地址赋值
	
	unsigned int checkSum = 0;		//校验和计算
    	for (int i = 0; i < 10; i++)
    	{
		checkSum += (unsigned short)(buffer[i*2]<<8 | buffer[i*2+1]);
        	checkSum %= 0xffff;
    	}
    	checkSum = htons(~(unsigned short)checkSum);	//取反 转为网络字节序
    	memcpy(buffer+10, &checkSum, sizeof(unsigned short));   //校验和赋值
    	memcpy(buffer + 20, pBuffer, len);			//数据赋值
    	ip_SendtoLower(buffer,len+20);			//发送
    	return 0;
}
