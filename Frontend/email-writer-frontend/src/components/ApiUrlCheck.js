import React from "react";

const ApiUrlCheck = () => {
  return (
    <div style={{ padding: "20px", background: "#f4f4f4", margin: "20px" }}>
      <h2>API URL Check</h2>
      <p>
        <strong>REACT_APP_API_URL:</strong>{" "}
        {process.env.REACT_APP_API_URL || "Not Found"}
      </p>
    </div>
  );
};

export default ApiUrlCheck;