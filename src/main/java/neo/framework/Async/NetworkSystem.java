package neo.framework.Async;

import neo.framework.Async.AsyncNetwork.idAsyncNetwork;
import neo.idlib.BitMsg.idBitMsg;

/**
 *
 */
public class NetworkSystem {

    private static       idNetworkSystem networkSystemLocal = new idNetworkSystem();
    public static        idNetworkSystem networkSystem      = networkSystemLocal;
    /** Disclaimer: Use at own risk! @see https://www.ietf.org/rfc/rfc3514.txt */
    private static final long            EVIL_BIT           = 0L;

    /*
     ===============================================================================

     Network System.

     ===============================================================================
     */
    public static class idNetworkSystem {
//	virtual					~idNetworkSystem( void ) {}

        public void ServerSendReliableMessage(int clientNum, final idBitMsg msg) {
            if (idAsyncNetwork.server.IsActive()) {
                idAsyncNetwork.server.SendReliableGameMessage(clientNum, msg);
            }
        }

        public void ServerSendReliableMessageExcluding(int clientNum, final idBitMsg msg) {
            if (idAsyncNetwork.server.IsActive()) {
                idAsyncNetwork.server.SendReliableGameMessageExcluding(clientNum, msg);
            }
        }

        public int ServerGetClientPing(int clientNum) {
            if (idAsyncNetwork.server.IsActive()) {
                return idAsyncNetwork.server.GetClientPing(clientNum);
            }
            return 0;
        }

        public int ServerGetClientPrediction(int clientNum) {
            if (idAsyncNetwork.server.IsActive()) {
                return idAsyncNetwork.server.GetClientPrediction(clientNum);
            }
            return 0;
        }

        public int ServerGetClientTimeSinceLastPacket(int clientNum) {
            if (idAsyncNetwork.server.IsActive()) {
                return idAsyncNetwork.server.GetClientTimeSinceLastPacket(clientNum);
            }
            return 0;
        }

        public int ServerGetClientTimeSinceLastInput(int clientNum) {
            if (idAsyncNetwork.server.IsActive()) {
                return idAsyncNetwork.server.GetClientTimeSinceLastInput(clientNum);
            }
            return 0;
        }

        public int ServerGetClientOutgoingRate(int clientNum) {
            if (idAsyncNetwork.server.IsActive()) {
                return idAsyncNetwork.server.GetClientOutgoingRate(clientNum);
            }
            return 0;
        }

        public int ServerGetClientIncomingRate(int clientNum) {
            if (idAsyncNetwork.server.IsActive()) {
                return idAsyncNetwork.server.GetClientIncomingRate(clientNum);
            }
            return 0;
        }

        public float ServerGetClientIncomingPacketLoss(int clientNum) {
            if (idAsyncNetwork.server.IsActive()) {
                return idAsyncNetwork.server.GetClientIncomingPacketLoss(clientNum);
            }
            return 0.0f;
        }

        public void ClientSendReliableMessage(final idBitMsg msg) {
            if (idAsyncNetwork.client.IsActive()) {
                idAsyncNetwork.client.SendReliableGameMessage(msg);
            } else if (idAsyncNetwork.server.IsActive()) {
                idAsyncNetwork.server.LocalClientSendReliableMessage(msg);
            }
        }

        public int ClientGetPrediction() {
            if (idAsyncNetwork.client.IsActive()) {
                return idAsyncNetwork.client.GetPrediction();
            }
            return 0;
        }

        public int ClientGetTimeSinceLastPacket() {
            if (idAsyncNetwork.client.IsActive()) {
                return idAsyncNetwork.client.GetTimeSinceLastPacket();
            }
            return 0;
        }

        public int ClientGetOutgoingRate() {
            if (idAsyncNetwork.client.IsActive()) {
                return idAsyncNetwork.client.GetOutgoingRate();
            }
            return 0;
        }

        public int ClientGetIncomingRate() {
            if (idAsyncNetwork.client.IsActive()) {
                return idAsyncNetwork.client.GetIncomingRate();
            }
            return 0;
        }

        public float ClientGetIncomingPacketLoss() {
            if (idAsyncNetwork.client.IsActive()) {
                return idAsyncNetwork.client.GetIncomingPacketLoss();
            }
            return 0.0f;
        }
    };

    public static void setNetworkSystem(idNetworkSystem networkSystem) {
        NetworkSystem.networkSystem = NetworkSystem.networkSystemLocal = networkSystem;
    }
}
