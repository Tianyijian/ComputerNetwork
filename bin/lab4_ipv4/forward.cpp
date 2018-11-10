/*
* THIS FILE IS FOR IP FORWARD TEST
*/
#include "sysInclude.h"
#include <map>
// system support
extern void fwd_LocalRcv(char *pBuffer, int length);

extern void fwd_SendtoLower(char *pBuffer, int length, unsigned int nexthop);

extern void fwd_DiscardPkt(char *pBuffer, int type);

extern unsigned int getIpv4Address( );

// implemented by students

map<unsigned int,unsigned int> routeTable;

void stud_Route_Init()
{
	routeTable.clear();
	printf("Init done!\n");
	return;
}

void stud_route_add(stud_route_msg *proute)
{
	int dstAddr = ntohl(proute->dest);
	int nexthop = ntohl(proute->nexthop);
	routeTable.insert(pair<int, int>(dstAddr, nexthop)); 
	//printf("dstAddr->%d \n", dstAddr);
	//printf("nexthop->%d \n", nexthop);
	return;
}


int stud_fwd_deal(char *pBuffer, int length)
{
	int ttl = (int)pBuffer[8]; 			//��� TTL
	int IHL = pBuffer[0]%16;     			//��� IP Head length
	int dstAddr = ntohl(*(unsigned int*)(pBuffer + 16));	//���Ŀ�ĵ�ַ
	if (dstAddr == getIpv4Address()) 	//�ж��Ƿ�Ϊ�������ܷ���
	{
		fwd_LocalRcv(pBuffer, length);
		return 0;
	}
	if (ttl <=0 )				//�ж�TTL�Ƿ����
	{
		fwd_DiscardPkt(pBuffer,STUD_FORWARD_TEST_TTLERROR);
		return 1;
	}
	printf("Want to find %d \n", dstAddr);
	map<unsigned int, unsigned int>::iterator iter;	//����·�ɱ�
	iter = routeTable.find(dstAddr);
	if (iter != routeTable.end())	//
	{
		printf("Find dstAddr! %d\n", iter->second);	
		//TTL��1 ���¸�ֵ
		//printf("ttl before %d \n", ttl);
		pBuffer[8] = (unsigned char)(ttl - 1);
		//printf("ttl after %d \n", (int)pBuffer[8]);
		//���¼���У���
	    	unsigned int sum = 0;	//����У���
    		for (int i = 0; i < IHL * 2; i++)
    		{
			if (i != 5)		//����У����ֶ�
			{
				sum += pBuffer[i*2]<<8 | pBuffer[i*2+1];
				sum %= 0xffff;
			}
    		}
		unsigned short checkSum = htons(~(unsigned short)sum);
		memcpy(pBuffer+10, &checkSum, sizeof(unsigned short));   //У��͸�ֵ
		fwd_SendtoLower(pBuffer, length, iter->second);
		return 0;
	}
	else		//�Ҳ��������÷���
	{
		printf("Not find, discard Packet!\n");
		fwd_DiscardPkt(pBuffer,STUD_FORWARD_TEST_NOROUTE);
		return 1;
	}
	return 0;
}

