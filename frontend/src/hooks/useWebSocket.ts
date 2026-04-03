import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/stores/authStore';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';

export const useWebSocket = () => {
  const { user, accessToken } = useAuthStore();
  const queryClient = useQueryClient();
  const clientRef = useRef<Client | null>(null);

  const connect = useCallback(() => {
    if (!user || !accessToken) return;

    const socket = new SockJS('/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      debug: (str) => {
        // console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = (frame) => {
      // console.log('Connected: ' + frame);
      
      // Subscribe to user-specific notification topic
      client.subscribe(`/topic/user.${user.id}.notifications`, (message) => {
        const notification = JSON.parse(message.body);
        
        // Show toast for new notification
        toast.info(notification.title, {
          description: notification.body,
        });

        // Invalidate notifications query to refetch
        queryClient.invalidateQueries({ queryKey: ['notifications'] });
        
        // If it's a budget alert, also invalidate dashboard and budgets
        if (notification.type === 'BUDGET_ALERT') {
          queryClient.invalidateQueries({ queryKey: ['dashboard'] });
          queryClient.invalidateQueries({ queryKey: ['budgets'] });
        }
      });
    };

    client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    client.activate();
    clientRef.current = client;
  }, [user, accessToken, queryClient]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
  }, []);

  useEffect(() => {
    connect();
    return () => disconnect();
  }, [connect, disconnect]);

  return { client: clientRef.current };
};
