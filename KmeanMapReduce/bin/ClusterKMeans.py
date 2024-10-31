import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score, davies_bouldin_score
from sklearn.model_selection import train_test_split
from sklearn import preprocessing

# Load and preprocess the dataset
df = pd.read_csv('./fruitNew.csv')

# Encoding categorical features (if necessary)
le = preprocessing.LabelEncoder()
df_encoded = df.apply(le.fit_transform)

# Split the dataset into training and testing sets
X_train, X_test = train_test_split(df_encoded, test_size=0.1, shuffle=True)

# We will use all the features for clustering (modify columns as needed)
X = X_train.iloc[:, :11].values 

# Find the optimal number of clusters using Silhouette Score and Davies-Bouldin Index
silhouette_scores = []  # To store silhouette scores for each k
davies_bouldin_scores = []  # To store Davies-Bouldin index for each k
k_values = range(2, 11)  # Test values of k from 2 to 10

for k in k_values:
    kmeans = KMeans(n_clusters=k, random_state=1, n_init=10)
    kmeans.fit(X)
    
    # Silhouette Score (for clustering quality)
    silhouette_avg = silhouette_score(X, kmeans.labels_)
    silhouette_scores.append(silhouette_avg)
    
    # Davies-Bouldin Index (for clustering separation)
    db_index = davies_bouldin_score(X, kmeans.labels_)
    davies_bouldin_scores.append(db_index)

# Plot Silhouette Scores vs. k
plt.figure(figsize=(10,5))
plt.plot(k_values, silhouette_scores, 'ro-', label='Silhouette Score')
plt.title('Silhouette Scores for Optimal k')
plt.xlabel('Number of clusters (k)')
plt.ylabel('Silhouette Score')
plt.xticks(k_values)
plt.grid(True)
plt.legend()
plt.show()

# Plot Davies-Bouldin Index vs. k
plt.figure(figsize=(10,5))
plt.plot(k_values, davies_bouldin_scores, 'bo-', label='Davies-Bouldin Index')
plt.title('Davies-Bouldin Index for Optimal k')
plt.xlabel('Number of clusters (k)')
plt.ylabel('Davies-Bouldin Index')
plt.xticks(k_values)
plt.grid(True)
plt.legend()
plt.show()

# Print out the results for best k based on Silhouette Score and Davies-Bouldin Index
best_k_silhouette = k_values[np.argmax(silhouette_scores)]
best_k_davies_bouldin = k_values[np.argmin(davies_bouldin_scores)]

print(f"The best number of clusters based on Silhouette Score is: {best_k_silhouette}")
print(f"The best number of clusters based on Davies-Bouldin Index is: {best_k_davies_bouldin}")
