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
	int version = pBuffer[0]/16;    //IP�汾��
	if (version != 4)    //���IP
	{
		printf("Wrong version is %d\n", version);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_VERSION_ERROR);
		return 1;
	}
	int IHL = pBuffer[0]%16;     //IP Head length
	if (IHL< 5)         //���IHL
	{
		printf("Wrong IHL is %d\n", IHL);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_HEADLEN_ERROR);
		return 1;
	}

 	int ttl = (int)pBuffer[8]; 			//TTL
	if (ttl == 0)   	  //���TTL
	{
		printf("Wrong ttl is %d\n", ttl);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_TTL_ERROR);
		return 1;
	}
	int dstAddr = ntohl(*(unsigned int*)(pBuffer + 16));    //Ŀ�ĵ�ַ תΪ�����ֽ���
	if (dstAddr != getIpv4Address() && dstAddr != 0xffff)
	{
		printf("Wrong dstAddr is 0x%x, localAddr is 0x%x\n", dstAddr,getIpv4Address());
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_DESTINATION_ERROR);    
        	return 1; 
	}
    	unsigned int sum = 0;	//����У���
    	for (int i = 0; i < IHL * 2; i++)
    	{
		if (i != 5)		//����У����ֶ�
		{
			sum += pBuffer[i*2]<<8 | pBuffer[i*2+1];
			sum %= 0xffff;
		}

    	}
	unsigned short calcCheckSum = ~(unsigned short)sum;
	unsigned short checkSum =  ntohs(*(unsigned short*)(pBuffer+10));		//ȡ��У����ֶ�
	if (calcCheckSum != checkSum)		//�����У�����У����ֶαȽ�
	{
		printf("Wrong checksum is 0x%x, should be 0x%x\n", checkSum, calcCheckSum);
		ip_DiscardPkt(pBuffer,STUD_IP_TEST_CHECKSUM_ERROR);
		return 1;
	}
	ip_SendtoUp(pBuffer,length);	//�������ݰ����ϲ�
	return 0;
}

int stud_ip_Upsend(char *pBuffer,unsigned short len,unsigned int srcAddr,
				   unsigned int dstAddr,byte protocol,byte ttl)
{
    	char *buffer = (char *)malloc((len + 20) * sizeof(char));    //����ռ�
    	memset(buffer, 0, len+20);  //��ʼ���ռ�Ϊ��
    	buffer[0] = 0x45;           //�汾���Լ�IHL
	unsigned short totalLength = htons(len+20);	//�ܳ��� תΪ�����ֽ���
    	memcpy(buffer+2, &totalLength, sizeof(unsigned short));  //�ܳ��ȸ�ֵ
    	buffer[8] = ttl;            //TTL
    	buffer[9] = protocol;       //Э��
	unsigned int srcAddress = htonl(srcAddr);		//Դ��ַ תΪ�����ֽ���
    	memcpy(buffer+12, &srcAddress, sizeof(unsigned int));  //Դ��ַ��ֵ
	unsigned int dstAddress = htonl(dstAddr);		//Ŀ�ĵ�ַ תΪ�����ֽ���
    	memcpy(buffer+16, &dstAddress, sizeof(unsigned int));  //Ŀ�ĵ�ַ��ֵ
	
	unsigned int checkSum = 0;		//У��ͼ���
    	for (int i = 0; i < 10; i++)
    	{
		checkSum += (unsigned short)(buffer[i*2]<<8 | buffer[i*2+1]);
        	checkSum %= 0xffff;
    	}
    	checkSum = htons(~(unsigned short)checkSum);	//ȡ�� תΪ�����ֽ���
    	memcpy(buffer+10, &checkSum, sizeof(unsigned short));   //У��͸�ֵ
    	memcpy(buffer + 20, pBuffer, len);			//���ݸ�ֵ
    	ip_SendtoLower(buffer,len+20);			//����
    	return 0;
}
